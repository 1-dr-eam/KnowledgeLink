package com.github.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderAddDTO;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Book;
import com.github.trade.entity.Order;
import com.github.trade.entity.ShoppingCar;
import com.github.trade.mapper.BookMapper;
import com.github.trade.mapper.CartMapper;
import com.github.trade.mapper.OrderMapper;
import com.github.trade.mq.OrderTimeoutProducer;
import com.github.trade.service.IOrderService;
import com.github.trade.util.TradeIdUtil;
import com.github.trade.util.UserItemInteractionRecordUtil;
import com.github.trade.vo.OrderDetailVO;
import com.github.trade.vo.OrderSynoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.github.common.utils.RedisConstant.BOOK_INFO_KEY;
import static com.github.common.utils.RedisConstant.BOOK_INFO_TTL;
import static com.github.common.utils.RedisConstant.ORDER_KEY;
import static com.github.common.utils.RedisConstant.ORDER_TTL;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements IOrderService {

    private static final String ORDER_STATUS_WAIT_PAY = "待支付";
    private static final String ORDER_STATUS_WAIT_SHIP = "待发货";
    private static final String ORDER_STATUS_WAIT_RECEIVE = "待收货";
    private static final String ORDER_STATUS_FINISHED = "已完成";
    private static final String ORDER_STATUS_REFUNDED = "已退款";
    private static final String ORDER_STATUS_CANCELLED = "已取消";
    private static final String ORDER_STATUS_TIMEOUT = "订单超时";
    private static final Set<String> VALID_ORDER_STATUS = new HashSet<>();
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        VALID_ORDER_STATUS.add(ORDER_STATUS_WAIT_PAY);
        VALID_ORDER_STATUS.add(ORDER_STATUS_WAIT_SHIP);
        VALID_ORDER_STATUS.add(ORDER_STATUS_WAIT_RECEIVE);
        VALID_ORDER_STATUS.add(ORDER_STATUS_FINISHED);
        VALID_ORDER_STATUS.add(ORDER_STATUS_REFUNDED);
        VALID_ORDER_STATUS.add(ORDER_STATUS_CANCELLED);
        VALID_ORDER_STATUS.add(ORDER_STATUS_TIMEOUT);
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText("if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end");
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final ExecutorService stockExecutor = new ThreadPoolExecutor(
            8,
            8,
            0L,
            TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1000),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Autowired
    private BookMapper bookMapper;
    @Autowired
    private BookServiceImpl bookServiceImpl;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private OrderTimeoutProducer orderTimeoutProducer;
    @Autowired
    private UserItemInteractionRecordUtil userItemInteractionRecordUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addOrder(OrderAddDTO orderAddDTO) {
        Long bookId = orderAddDTO == null ? null : TradeIdUtil.parseId(orderAddDTO.getBookId());
        if (bookId == null || orderAddDTO.getCount() == null || orderAddDTO.getCount() <= 0) {
            return Result.error("订单参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        List<OrderCreateItem> createItems = new ArrayList<>();
        createItems.add(new OrderCreateItem(null, bookId, orderAddDTO.getCount(), orderAddDTO.getAddress()));
        return createOrders(userId, createItems, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addOrderBatch(BatchIdRequest batchIdRequest) {
        List<Long> cartIds = batchIdRequest == null ? new ArrayList<>() : TradeIdUtil.parseIds(batchIdRequest.getIds());
        String orderAddress = batchIdRequest == null ? null : batchIdRequest.getAddress();
        if (cartIds.isEmpty() || orderAddress == null || orderAddress.isBlank()) {
            return Result.error("订单参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        List<ShoppingCar> cartList = cartMapper.selectList(
                new LambdaQueryWrapper<ShoppingCar>()
                        .eq(ShoppingCar::getUserId, userId)
                        .in(ShoppingCar::getId, cartIds)
        );
        if (cartList.isEmpty()) {
            return Result.error("待结算商品不存在");
        }

        List<OrderCreateItem> createItems = new ArrayList<>();
        for (ShoppingCar cart : cartList) {
            if (cart.getBookId() == null || cart.getCount() == null || cart.getCount() <= 0) {
                return Result.error("存在无效结算商品");
            }
            createItems.add(new OrderCreateItem(cart.getId(), cart.getBookId(), cart.getCount(), orderAddress));
        }
        return createOrders(userId, createItems, true);
    }

    @Override
    public Result getOrderById(IdRequest idRequest) {
        Long orderId = idRequest == null ? null : TradeIdUtil.parseId(idRequest.getId());
        if (orderId == null) {
            return Result.error("订单参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        Order order = getOrderByIdInternal(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!userId.equals(order.getUserId()) && !userId.equals(order.getSellerId())) {
            return Result.error("无权查看该订单");
        }
        return Result.success(toOrderDetailVO(order));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelOrder(IdRequest idRequest) {
        Long orderId = idRequest == null ? null : TradeIdUtil.parseId(idRequest.getId());
        if (orderId == null) {
            return Result.error("订单参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        boolean isBuyer = userId.equals(order.getUserId());
        boolean isSeller = userId.equals(order.getSellerId());
        if (!isBuyer && !isSeller) {
            return Result.error("无权取消该订单");
        }

        if (isBuyer && (ORDER_STATUS_WAIT_PAY.equals(order.getStatus()) || ORDER_STATUS_WAIT_SHIP.equals(order.getStatus()))) {
            return doCancelOrder(order);
        }
        if (isSeller && ORDER_STATUS_WAIT_SHIP.equals(order.getStatus())) {
            return doCancelOrder(order);
        }
        return Result.error("当前状态不可取消");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteOrder(IdRequest idRequest) {
        Long orderId = idRequest == null ? null : TradeIdUtil.parseId(idRequest.getId());
        if (orderId == null) {
            return Result.error("订单参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!userId.equals(order.getUserId()) && !userId.equals(order.getSellerId())) {
            return Result.error("无权删除该订单");
        }
        if (ORDER_STATUS_WAIT_PAY.equals(order.getStatus()) || ORDER_STATUS_WAIT_SHIP.equals(order.getStatus()) || ORDER_STATUS_WAIT_RECEIVE.equals(order.getStatus())) {
            return Result.error("当前状态不可删除");
        }
        baseMapper.deleteById(order.getId());
        deleteOrderCache(order.getId());
        return Result.success();
    }

    @Override
    public Result getAllConsumerOrder() {
        Long userId = UserHolder.getUser().getId();
        List<Order> orderList = baseMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getUserId, userId)
                        .orderByDesc(Order::getCreateTime)
        );
        return Result.success(toOrderSynoVOList(orderList));
    }

    @Override
    public Result getAllSellerOrder() {
        Long sellerId = UserHolder.getUser().getId();
        List<Order> orderList = baseMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, sellerId)
                        .orderByDesc(Order::getCreateTime)
        );
        return Result.success(toOrderSynoVOList(orderList));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateOrderStatus(OrderStatusDTO orderStatusDTO) {
        Long orderId = orderStatusDTO == null ? null : TradeIdUtil.parseId(orderStatusDTO.getId());
        String targetStatus = orderStatusDTO == null ? null : orderStatusDTO.getStatus();
        if (orderId == null || targetStatus == null || !VALID_ORDER_STATUS.contains(targetStatus)) {
            return Result.error("订单参数错误");
        }
        if (ORDER_STATUS_TIMEOUT.equals(targetStatus) || ORDER_STATUS_WAIT_SHIP.equals(targetStatus)) {
            return Result.error("该状态暂由后续任务驱动");
        }
        Long userId = UserHolder.getUser().getId();
        Order order = baseMapper.selectById(orderId);
        if (order == null) {
            return Result.error("订单不存在");
        }
        boolean isBuyer = userId.equals(order.getUserId());
        boolean isSeller = userId.equals(order.getSellerId());
        if (!isBuyer && !isSeller) {
            return Result.error("无权修改该订单");
        }

        if (ORDER_STATUS_CANCELLED.equals(targetStatus)) {
            return cancelOrder(orderStatusToId(orderStatusDTO));
        }
        if (ORDER_STATUS_FINISHED.equals(targetStatus)) {
            if (!isBuyer || !ORDER_STATUS_WAIT_RECEIVE.equals(order.getStatus())) {
                return Result.error("当前状态不可修改为已完成");
            }
            order.setStatus(ORDER_STATUS_FINISHED);
            order.setFinishedTime(LocalDateTime.now());
            baseMapper.updateById(order);
            syncOrderCache(order);
            return Result.success();
        }
        if (ORDER_STATUS_WAIT_RECEIVE.equals(targetStatus)) {
            if (!isSeller || !ORDER_STATUS_WAIT_SHIP.equals(order.getStatus())) {
                return Result.error("当前状态不可修改为待收货");
            }
            order.setStatus(ORDER_STATUS_WAIT_RECEIVE);
            baseMapper.updateById(order);
            syncOrderCache(order);
            return Result.success();
        }
        if (ORDER_STATUS_REFUNDED.equals(targetStatus)) {
            if (!isSeller) {
                return Result.error("仅商户可发起退款");
            }
            if (!ORDER_STATUS_WAIT_PAY.equals(order.getStatus())
                    && !ORDER_STATUS_WAIT_SHIP.equals(order.getStatus())
                    && !ORDER_STATUS_WAIT_RECEIVE.equals(order.getStatus())) {
                return Result.error("当前状态不可修改为已退款");
            }
            order.setStatus(ORDER_STATUS_REFUNDED);
            order.setFinishedTime(LocalDateTime.now());
            baseMapper.updateById(order);
            syncOrderCache(order);
            restoreBookStock(order.getBookId(), order.getCount());
            return Result.success();
        }
        return Result.error("当前状态不可修改");
    }

    private Result createOrders(Long userId, List<OrderCreateItem> createItems, boolean clearCartAfterOrder) {
        if (createItems == null || createItems.isEmpty()) {
            return Result.error("订单参数错误");
        }

        Map<Long, Integer> needCountMap = new LinkedHashMap<>();
        for (OrderCreateItem item : createItems) {
            if (item.bookId == null || item.count == null || item.count <= 0) {
                return Result.error("存在无效结算商品");
            }
            needCountMap.merge(item.bookId, item.count, Integer::sum);
        }

        Map<Long, Book> bookMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : needCountMap.entrySet()) {
            Book book = bookMapper.selectById(entry.getKey());
            if (book == null) {
                return Result.error("商品信息不存在请重试");
            }
            if (book.getCount() == null || book.getCount() < entry.getValue()) {
                return Result.error("商品库存不足");
            }
            bookMap.put(entry.getKey(), book);
        }

        Map<Long, Integer> deductedMap = new LinkedHashMap<>();
        List<Future<Boolean>> futureList = new ArrayList<>();
        List<Long> bookIdOrder = new ArrayList<>(needCountMap.keySet());
        for (Long bookId : bookIdOrder) {
            Integer count = needCountMap.get(bookId);
            Callable<Boolean> task = () -> subBookCount(bookId, count);
            futureList.add(stockExecutor.submit(task));
        }

        try {
            for (int i = 0; i < futureList.size(); i++) {
                boolean success = Boolean.TRUE.equals(futureList.get(i).get());
                Long bookId = bookIdOrder.get(i);
                if (!success) {
                    rollbackDeductedStock(deductedMap);
                    return Result.error("商品库存不足");
                }
                deductedMap.put(bookId, needCountMap.get(bookId));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            rollbackDeductedStock(deductedMap);
            return Result.error("下单处理中断");
        } catch (ExecutionException e) {
            rollbackDeductedStock(deductedMap);
            return Result.error("下单失败，请稍后重试");
        }

        List<String> createdOrderIds = new ArrayList<>();
        try {
            for (OrderCreateItem item : createItems) {
                Book book = bookMap.get(item.bookId);
                Order order = new Order();
                order.setUserId(userId);
                order.setSellerId(book.getSellerId());
                order.setBookId(book.getItemId());
                order.setPrice(book.getPrice());
                order.setCount(item.count);
                order.setTotalPrice(book.getPrice() * item.count);
                order.setBookName(book.getName());
                order.setStatus(ORDER_STATUS_WAIT_PAY);
                order.setAddress(item.address == null ? "" : item.address);
                baseMapper.insert(order);
                createdOrderIds.add(String.valueOf(order.getId()));
                syncOrderCache(order);
                orderTimeoutProducer.sendOrderTimeoutCheck(order.getId());
                userItemInteractionRecordUtil.recordBuy(userId, book.getItemId());
                if (clearCartAfterOrder && item.cartId != null) {
                    cartMapper.deleteById(item.cartId);
                }
            }
        } catch (Exception e) {
            rollbackDeductedStock(deductedMap);
            throw e;
        }

        return Result.success(createdOrderIds);
    }

    /**
     * 扣减库存：先获取分布式锁，再二次校验库存，再执行原子扣减。
     */
    private boolean subBookCount(Long bookId, Integer count) {
        if (bookId == null || count == null || count <= 0) {
            return false;
        }
        String lockValue = UUID.randomUUID().toString();
        if (!tryLockBookStock(bookId, lockValue)) {
            return false;
        }
        try {
            Book latestBook = bookMapper.selectById(bookId);
            if (latestBook == null || latestBook.getCount() == null || latestBook.getCount() < count) {
                return false;
            }
            int affectedRows = bookMapper.update(
                    null,
                    new LambdaUpdateWrapper<Book>()
                            .eq(Book::getItemId, bookId)
                            .ge(Book::getCount, count)
                            .setSql("count = count - " + count)
            );
            if (affectedRows <= 0) {
                return false;
            }
            Book updatedBook = bookMapper.selectById(bookId);
            syncBookCache(updatedBook);
            return true;
        } finally {
            unlockBookStock(bookId, lockValue);
        }
    }

    private void rollbackDeductedStock(Map<Long, Integer> deductedMap) {
        for (Map.Entry<Long, Integer> entry : deductedMap.entrySet()) {
            restoreBookStock(entry.getKey(), entry.getValue());
        }
    }

    private Result doCancelOrder(Order order) {
        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setFinishedTime(LocalDateTime.now());
        baseMapper.updateById(order);
        restoreBookStock(order.getBookId(), order.getCount());
        syncOrderCache(order);
        return Result.success();
    }

    private IdRequest orderStatusToId(OrderStatusDTO orderStatusDTO) {
        IdRequest idRequest = new IdRequest();
        idRequest.setId(orderStatusDTO.getId());
        return idRequest;
    }

    private List<OrderSynoVO> toOrderSynoVOList(List<Order> orderList) {
        List<OrderSynoVO> resultList = new ArrayList<>();
        for (Order order : orderList) {
            try {
                resultList.add(toOrderSynoVO(order));
            } catch (Exception e) {
                resultList.add(BeanUtil.copyProperties(order, OrderSynoVO.class));
            }
        }
        return resultList;
    }

    private OrderSynoVO toOrderSynoVO(Order order) {
        OrderSynoVO orderSynoVO = BeanUtil.copyProperties(order, OrderSynoVO.class);
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(order.getBookId());
        if (bookDTO != null) {
            orderSynoVO.setBookAuthor(bookDTO.getAuthor());
            orderSynoVO.setBookPublisher(bookDTO.getPublisher());
            orderSynoVO.setBookVersion(bookDTO.getVersion());
            if (bookDTO.getImage() != null && !bookDTO.getImage().isEmpty()) {
                orderSynoVO.setImage(bookDTO.getImage());
            }
        }
        return orderSynoVO;
    }

    private OrderDetailVO toOrderDetailVO(Order order) {
        OrderDetailVO orderDetailVO = BeanUtil.copyProperties(order, OrderDetailVO.class);
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(order.getBookId());
        if (bookDTO != null) {
            orderDetailVO.setBookAuthor(bookDTO.getAuthor());
            orderDetailVO.setBookPublisher(bookDTO.getPublisher());
            orderDetailVO.setBookVersion(bookDTO.getVersion());
            orderDetailVO.setBookDescription(bookDTO.getDescription());
            if (bookDTO.getImage() != null && !bookDTO.getImage().isEmpty()) {
                orderDetailVO.setImage(bookDTO.getImage());
            }
        }
        return orderDetailVO;
    }

    private void syncBookCache(Book book) {
        if (book == null) {
            return;
        }
        String bookKey = BOOK_INFO_KEY + book.getItemId();
        String bookJson = JSONUtil.toJsonStr(book);
        stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
    }

    private void syncOrderCache(Order order) {
        String tokenKey = ORDER_KEY + order.getId();
        String orderJson = JSONUtil.toJsonStr(order);
        stringRedisTemplate.opsForValue().set(tokenKey, orderJson, ORDER_TTL, TimeUnit.MINUTES);
    }

    private void deleteOrderCache(Long orderId) {
        String tokenKey = ORDER_KEY + orderId;
        stringRedisTemplate.delete(tokenKey);
    }

    private Order getOrderByIdInternal(Long orderId) {
        String tokenKey = ORDER_KEY + orderId;
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(tokenKey))) {
            String orderJson = stringRedisTemplate.opsForValue().get(tokenKey);
            if (orderJson != null) {
                return JSONUtil.toBean(orderJson, Order.class);
            }
        }
        Order order = baseMapper.selectById(orderId);
        if (order != null) {
            syncOrderCache(order);
        }
        return order;
    }

    private void restoreBookStock(Long bookId, Integer count) {
        if (bookId == null || count == null || count <= 0) {
            return;
        }
        bookMapper.update(
                null,
                new LambdaUpdateWrapper<Book>()
                        .eq(Book::getItemId, bookId)
                        .setSql("count = count + " + count)
        );
        Book latestBook = bookMapper.selectById(bookId);
        syncBookCache(latestBook);
    }

    private boolean tryLockBookStock(Long bookId, String lockValue) {
        String lockKey = getBookStockLockKey(bookId);
        Boolean lockSuccess = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(lockSuccess);
    }

    private void unlockBookStock(Long bookId, String lockValue) {
        String lockKey = getBookStockLockKey(bookId);
        stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockValue);
    }

    private String getBookStockLockKey(Long bookId) {
        return "KnowledgeLink:trade:bookStockLock:" + bookId;
    }

    private static class OrderCreateItem {
        private final Long cartId;
        private final Long bookId;
        private final Integer count;
        private final String address;

        private OrderCreateItem(Long cartId, Long bookId, Integer count, String address) {
            this.cartId = cartId;
            this.bookId = bookId;
            this.count = count;
            this.address = address;
        }
    }
}
