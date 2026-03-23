package com.github.trade.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Book;
import com.github.trade.entity.Order;
import com.github.trade.mapper.BookMapper;
import com.github.trade.mapper.SellerOrderMapper;
import com.github.trade.service.ISellerOrderService;
import com.github.trade.vo.OrderDetailVO;
import com.github.trade.vo.OrderSynoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.github.trade.util.RedisConstant.BOOK_INFO_KEY;
import static com.github.trade.util.RedisConstant.BOOK_INFO_TTL;
import static com.github.trade.util.RedisConstant.ORDER_KEY;
import static com.github.trade.util.RedisConstant.ORDER_TTL;

/**
 * 用户作为商户订单service实现类
 *
 * @author ning
 * @date 2026/03/19
 */
@Service
public class SellerOrderServiceImpl extends ServiceImpl<SellerOrderMapper, Order> implements ISellerOrderService {
    private static final String ORDER_STATUS_WAIT_PAY = "待支付";
    private static final String ORDER_STATUS_WAIT_SHIP = "待发货";
    private static final String ORDER_STATUS_WAIT_RECEIVE = "待收货";
    private static final String ORDER_STATUS_FINISHED = "已完成";
    private static final String ORDER_STATUS_REFUNDED = "已退款";
    private static final String ORDER_STATUS_CANCELLED = "已取消";
    private static final String ORDER_STATUS_TIMEOUT = "订单超时";
    private static final Set<String> VALID_ORDER_STATUS = new HashSet<>();

    static {
        VALID_ORDER_STATUS.add(ORDER_STATUS_WAIT_PAY);
        VALID_ORDER_STATUS.add(ORDER_STATUS_WAIT_SHIP);
        VALID_ORDER_STATUS.add(ORDER_STATUS_WAIT_RECEIVE);
        VALID_ORDER_STATUS.add(ORDER_STATUS_FINISHED);
        VALID_ORDER_STATUS.add(ORDER_STATUS_REFUNDED);
        VALID_ORDER_STATUS.add(ORDER_STATUS_CANCELLED);
        VALID_ORDER_STATUS.add(ORDER_STATUS_TIMEOUT);
    }

    @Autowired
    private BookServiceImpl bookServiceImpl;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 查看所有售出的订单
     *
     * @return 订单列表
     */
    @Override
    public Result getAllSellerOrder() {
        Long sellerId = UserHolder.getUser().getId();
        List<Order> orderList = baseMapper.selectList(
                new LambdaQueryWrapper<Order>()
                        .eq(Order::getSellerId, sellerId)
                        .orderByDesc(Order::getCreateTime)
        );
        List<OrderSynoVO> resultList = new ArrayList<>();
        for (Order order : orderList) {
            resultList.add(toOrderSynoVO(order));
        }
        return Result.success(resultList);
    }

    /**
     * 根据id查看对应的订单信息
     *
     * @param idRequest id请求dto
     * @return 订单信息
     */
    @Override
    public Result getSellerOrderById(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("订单参数错误");
        }
        Long sellerId = UserHolder.getUser().getId();
        Order order = getOrderById(idRequest.getId());
        if (order == null || !sellerId.equals(order.getSellerId())) {
            return Result.error("订单不存在");
        }
        return Result.success(toOrderDetailVO(order));
    }

    /**
     * 取消订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelSellerOrder(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("订单参数错误");
        }
        Long sellerId = UserHolder.getUser().getId();
        Order order = baseMapper.selectById(idRequest.getId());
        if (order == null || !sellerId.equals(order.getSellerId())) {
            return Result.error("订单不存在");
        }
        if (!ORDER_STATUS_WAIT_SHIP.equals(order.getStatus())) {
            return Result.error("当前状态不可取消");
        }
        order.setStatus(ORDER_STATUS_CANCELLED);
        order.setFinishedTime(LocalDateTime.now());
        baseMapper.updateById(order);
        restoreBookStock(order.getBookId(), order.getCount());
        syncOrderCache(order);
        return Result.success();
    }

    /**
     * 删除订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteSellerOrder(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("订单参数错误");
        }
        Long sellerId = UserHolder.getUser().getId();
        Order order = baseMapper.selectById(idRequest.getId());
        if (order == null || !sellerId.equals(order.getSellerId())) {
            return Result.error("订单不存在");
        }
        if (ORDER_STATUS_WAIT_PAY.equals(order.getStatus()) || ORDER_STATUS_WAIT_SHIP.equals(order.getStatus()) || ORDER_STATUS_WAIT_RECEIVE.equals(order.getStatus())) {
            return Result.error("当前状态不可删除");
        }
        baseMapper.deleteById(order.getId());
        deleteOrderCache(order.getId());
        return Result.success();
    }

    /**
     * 修改订单状态
     *
     * @param orderStatusDTO 订单状态dto
     * @return success
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateSellerOrderStatus(OrderStatusDTO orderStatusDTO) {
        if (orderStatusDTO == null || orderStatusDTO.getId() == null || orderStatusDTO.getStatus() == null) {
            return Result.error("订单参数错误");
        }
        if (!VALID_ORDER_STATUS.contains(orderStatusDTO.getStatus())) {
            return Result.error("订单状态非法");
        }
        if (ORDER_STATUS_WAIT_SHIP.equals(orderStatusDTO.getStatus()) || ORDER_STATUS_TIMEOUT.equals(orderStatusDTO.getStatus())) {
            return Result.error("该状态暂由后续定时任务处理");
        }
        if (ORDER_STATUS_FINISHED.equals(orderStatusDTO.getStatus())) {
            return Result.error("已完成状态只能由用户修改");
        }
        Long sellerId = UserHolder.getUser().getId();
        Order order = baseMapper.selectById(orderStatusDTO.getId());
        if (order == null || !sellerId.equals(order.getSellerId())) {
            return Result.error("订单不存在");
        }
        String targetStatus = orderStatusDTO.getStatus();
        if (ORDER_STATUS_CANCELLED.equals(targetStatus)) {
            if (!ORDER_STATUS_WAIT_SHIP.equals(order.getStatus())) {
                return Result.error("当前状态不可取消");
            }
            order.setStatus(ORDER_STATUS_CANCELLED);
            order.setFinishedTime(LocalDateTime.now());
            baseMapper.updateById(order);
            restoreBookStock(order.getBookId(), order.getCount());
            syncOrderCache(order);
            return Result.success();
        }
        if (ORDER_STATUS_WAIT_RECEIVE.equals(targetStatus)) {
            if (!ORDER_STATUS_WAIT_SHIP.equals(order.getStatus())) {
                return Result.error("当前状态不可修改为待收货");
            }
            order.setStatus(ORDER_STATUS_WAIT_RECEIVE);
            baseMapper.updateById(order);
            syncOrderCache(order);
            return Result.success();
        }
        if (ORDER_STATUS_REFUNDED.equals(targetStatus)) {
            if (!ORDER_STATUS_WAIT_PAY.equals(order.getStatus()) && !ORDER_STATUS_WAIT_SHIP.equals(order.getStatus()) && !ORDER_STATUS_WAIT_RECEIVE.equals(order.getStatus())) {
                return Result.error("当前状态不可修改为已退款");
            }
            order.setStatus(ORDER_STATUS_REFUNDED);
            order.setFinishedTime(LocalDateTime.now());
            baseMapper.updateById(order);
            syncOrderCache(order);
            return Result.success();
        }
        return Result.error("该状态仅支持用户修改");
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

    private Order getOrderById(Long orderId) {
        String tokenKey = ORDER_KEY + orderId;
        if (stringRedisTemplate.hasKey(tokenKey)) {
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

    private OrderSynoVO toOrderSynoVO(Order order) {
        OrderSynoVO orderSynoVO = BeanUtil.copyProperties(order, OrderSynoVO.class);
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(order.getBookId());
        if (bookDTO != null) {
            orderSynoVO.setBookAuthor(bookDTO.getAuthor());
            orderSynoVO.setBookPublisher(bookDTO.getPublisher());
            orderSynoVO.setBookVersion(bookDTO.getVersion());
            if (bookDTO.getAvatar() != null && !bookDTO.getAvatar().isEmpty()) {
                orderSynoVO.setImage(bookDTO.getAvatar().get(0));
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
            if (bookDTO.getAvatar() != null && !bookDTO.getAvatar().isEmpty()) {
                orderDetailVO.setImage(bookDTO.getAvatar().get(0));
            }
        }
        return orderDetailVO;
    }

    private void restoreBookStock(Long bookId, Integer count) {
        if (bookId == null || count == null || count <= 0) {
            return;
        }
        bookMapper.update(null, new LambdaUpdateWrapper<Book>()
                .eq(Book::getId, bookId)
                .setSql("count = count + " + count));
        Book latestBook = bookMapper.selectById(bookId);
        syncBookCache(latestBook);
    }

    private void syncBookCache(Book book) {
        if (book == null) {
            return;
        }
        String bookKey = BOOK_INFO_KEY + book.getId();
        String bookJson = JSONUtil.toJsonStr(book);
        stringRedisTemplate.opsForValue().set(bookKey, bookJson, BOOK_INFO_TTL, TimeUnit.MINUTES);
    }
}
