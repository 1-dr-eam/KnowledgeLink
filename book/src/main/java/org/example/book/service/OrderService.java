package org.example.book.service;

import learning_exchange_platform.model.User;
import org.example.book.entity.Book;
import org.example.book.entity.Order;
import org.example.book.mapper.OrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Transactional
    public int purchaseBook(Integer bookId, Integer buyerId) {
        // 获取图书信息
        Book book = orderMapper.getBookById(bookId);
        if (book == null) {
            return -1; // 图书不存在
        }

        // 检查图书是否已被购买（status != 1）
        if (book.getStatus() != 1) {
            return -2; // 图书已下架或已被购买
        }

        // 获取买家信息
        User buyer = orderMapper.getUserById(buyerId);
        if (buyer == null) {
            return -3; // 买家不存在
        }

        // 新增检查：不能购买自己发布的图书
        if (book.getSellerId().equals(buyerId)) {
            return -5; // 不能购买自己发布的图书
        }

        // 使用BigDecimal进行精确计算
        BigDecimal bookPrice = BigDecimal.valueOf(book.getPrice());
        BigDecimal buyerBalance = BigDecimal.valueOf(buyer.getBalance());

        if (buyerBalance.compareTo(bookPrice) < 0) {
            return -4; // 余额不足
        }

        // 计算新余额
        BigDecimal newBalance = buyerBalance.subtract(bookPrice);

        // 获取卖家ID（从图书信息中）
        Integer sellerId = book.getSellerId();

        // 创建订单
        Order order = new Order();
        order.setBookId(bookId);
        order.setBookName(book.getName());
        order.setSellerId(sellerId);
        order.setBuyerId(buyerId);
        order.setPrice(book.getPrice()); // 保持原来的double类型
        order.setStatus("买家已下单");
        order.setCreateTime(LocalDateTime.now().toString());
        order.setFinishedTime(LocalDateTime.now().toString());
        order.setAddress(null);
        order.setDeliveryTime(null);

        // 保存订单
        int result = orderMapper.createOrder(order);
        if (result > 0) {
            // 扣除买家余额
            int updateBalanceResult = orderMapper.updateUserBalance(buyerId, newBalance.doubleValue());

            if (updateBalanceResult > 0) {
                // 余额扣除成功，更新图书状态为2（交易中）
                orderMapper.updateBookStatus(bookId, 2);
                return result;
            } else {
                // 余额扣除失败，抛出异常回滚事务
                throw new RuntimeException("余额扣除失败");
            }
        }
        return 0; // 订单创建失败
    }

    
    @Transactional
    public int buyerCancelOrder(Integer orderId, Integer buyerId) {
        try {
            System.out.println("买家取消订单，orderId: " + orderId + ", buyerId: " + buyerId);

            // 1. 验证订单是否存在且属于该买家
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                System.out.println("订单不存在");
                return -1; // 订单不存在
            }

            if (!order.getBuyerId().equals(buyerId)) {
                System.out.println("订单不属于该买家，订单buyerId: " + order.getBuyerId() + ", 传入buyerId: " + buyerId);
                return -2; // 订单不属于该买家
            }

            // 2. 检查订单状态是否为"买家已下单"（卖家未发货前）
            if (!"买家已下单".equals(order.getStatus())) {
                System.out.println("订单状态不允许取消，当前状态: " + order.getStatus());
                return -3; // 订单状态不允许取消
            }

            Integer bookId = order.getBookId();
            Double orderPrice = order.getPrice();

            System.out.println("图书ID: " + bookId + ", 订单金额: " + orderPrice);

            // 3. 恢复买家余额
            int balanceResult = orderMapper.addUserBalance(buyerId, orderPrice);
            System.out.println("恢复余额结果: " + balanceResult);
            if (balanceResult <= 0) {
                return -4; // 恢复余额失败
            }

            // 4. 更新图书状态为1（重新上架）
            int bookResult = orderMapper.updateBookStatus(bookId, 1);
            System.out.println("更新图书状态结果: " + bookResult);
            if (bookResult <= 0) {
                return -5; // 更新图书状态失败
            }

            // 5. 更新订单状态为"买家取消交易"并设置完成时间
            int orderResult = orderMapper.updateOrderStatusAndFinishedTime(orderId, "买家取消交易", LocalDateTime.now().toString());
            System.out.println("更新订单状态结果: " + orderResult);
            if (orderResult <= 0) {
                return -6; // 更新订单状态失败
            }

            System.out.println("买家取消交易成功");
            return 1; // 取消成功

        } catch (Exception e) {
            System.out.println("买家取消订单异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("买家取消订单过程中发生错误: " + e.getMessage());
        }
    }

    
    public int shipOrder(Integer orderId) {
        // 更新订单状态为卖家已发货
        Order order = orderMapper.getOrderById(orderId);
        if (order == null) {
            System.out.println("订单不存在");
            return 0; // 订单不存在
        }

        if (!"买家已下单".equals(order.getStatus())) {
            System.out.println("订单状态不允许发货，当前状态: " + order.getStatus());
            return 0; // 订单状态不允许发货
        }

        int updateResult = orderMapper.updateOrderStatus(orderId, "卖家已发货");
        if (updateResult <= 0) {
            System.out.println("更新订单状态失败");
            return 0; // 更新订单状态失败
        }

        return 1;  // 发货成功
    }

    
    @Transactional
    public int cancelOrder(Integer orderId, Integer bookId, Integer sellerId) {
        try {
            System.out.println("开始取消订单，orderId: " + orderId + ", bookId: " + bookId + ", sellerId: " + sellerId);

            // 1. 验证订单是否存在且属于该卖家
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                System.out.println("订单不存在");
                return -1; // 订单不存在
            }

            if (!order.getSellerId().equals(sellerId)) {
                System.out.println("订单不属于该卖家，订单sellerId: " + order.getSellerId() + ", 传入sellerId: " + sellerId);
                return -2; // 订单不属于该卖家
            }

            if (!order.getBookId().equals(bookId)) {
                System.out.println("订单与图书不匹配，订单bookId: " + order.getBookId() + ", 传入bookId: " + bookId);
                return -3; // 订单与图书不匹配
            }

            if (!"买家已下单".equals(order.getStatus())) {
                System.out.println("订单状态不允许取消，当前状态: " + order.getStatus());
                return -4; // 订单状态不允许取消
            }

            Integer buyerId = order.getBuyerId();
            Double orderPrice = order.getPrice();

            System.out.println("买家ID: " + buyerId + ", 订单金额: " + orderPrice);

            // 2. 恢复买家余额
            int balanceResult = orderMapper.addUserBalance(buyerId, orderPrice);
            System.out.println("恢复余额结果: " + balanceResult);
            if (balanceResult <= 0) {
                return -5; // 恢复余额失败
            }

            // 3. 更新图书状态为1（重新上架）
            int bookResult = orderMapper.updateBookStatus(bookId, 1);
            System.out.println("更新图书状态结果: " + bookResult);
            if (bookResult <= 0) {
                return -6; // 更新图书状态失败
            }

            // 4. 更新订单状态为"卖家取消交易"并设置完成时间
            int orderResult = orderMapper.updateOrderStatusAndFinishedTime(orderId, "卖家取消交易", LocalDateTime.now().toString());
            System.out.println("更新订单状态结果: " + orderResult);
            if (orderResult <= 0) {
                return -7; // 更新订单状态失败
            }

            System.out.println("卖家取消交易成功");
            return 1; // 取消成功

        } catch (Exception e) {
            System.out.println("取消订单异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("取消订单过程中发生错误: " + e.getMessage());
        }
    }

    
    @Transactional
    public int confirmReceipt(Integer orderId, Integer buyerId) {
        try {
            System.out.println("确认收货，orderId: " + orderId + ", buyerId: " + buyerId);

            // 1. 验证订单是否存在且属于该买家
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                System.out.println("订单不存在");
                return -1; // 订单不存在
            }

            if (!order.getBuyerId().equals(buyerId)) {
                System.out.println("订单不属于该买家");
                return -2; // 订单不属于该买家
            }

            // 2. 检查订单状态是否为"卖家已发货"
            if (!"卖家已发货".equals(order.getStatus())) {
                System.out.println("订单状态不允许确认收货，当前状态: " + order.getStatus());
                return -3; // 订单状态不允许确认收货
            }

            // 3. 确认收货，更新订单状态
            int confirmResult = orderMapper.confirmReceipt(orderId, buyerId);
            System.out.println("确认收货结果: " + confirmResult);
            if (confirmResult <= 0) {
                return -4; // 确认收货失败
            }

            // 4. 将书款转给卖家
            int transferResult = orderMapper.transferToSeller(order.getSellerId(), order.getPrice());
            System.out.println("转账给卖家结果: " + transferResult);
            if (transferResult <= 0) {
                return -5; // 转账失败
            }

            // 5. 更新图书状态为3（已售出）
            int bookStatusResult = orderMapper.updateBookStatus(order.getBookId(), 3);
            System.out.println("更新图书状态为3（已售出）结果: " + bookStatusResult);
            if (bookStatusResult <= 0) {
                return -6; // 更新图书状态失败
            }

            System.out.println("确认收货成功，交易完成，图书状态已更新为已售出");
            return 1; // 成功

        } catch (Exception e) {
            System.out.println("确认收货异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("确认收货过程中发生错误: " + e.getMessage());
        }
    }

    
    @Transactional
    public int disputeOrder(Integer orderId, Integer buyerId) {
        try {
            System.out.println("申请纠纷介入，orderId: " + orderId + ", buyerId: " + buyerId);

            // 1. 验证订单是否存在且属于该买家
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                System.out.println("订单不存在");
                return -1; // 订单不存在
            }

            if (!order.getBuyerId().equals(buyerId)) {
                System.out.println("订单不属于该买家");
                return -2; // 订单不属于该买家
            }

            // 2. 检查订单状态是否为"卖家已发货"
            if (!"卖家已发货".equals(order.getStatus())) {
                System.out.println("订单状态不允许申请纠纷，当前状态: " + order.getStatus());
                return -3; // 订单状态不允许申请纠纷
            }

            // 3. 更新订单状态为"交易纠纷中"
            int disputeResult = orderMapper.disputeOrder(orderId, buyerId);
            System.out.println("申请纠纷介入结果: " + disputeResult);
            if (disputeResult <= 0) {
                return -4; // 申请纠纷失败
            }

            System.out.println("申请纠纷介入成功，等待客服处理");
            return 1; // 成功

        } catch (Exception e) {
            System.out.println("申请纠纷介入异常: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("申请纠纷介入过程中发生错误: " + e.getMessage());
        }
    }

    
    public List<Order> getOrderInfo(Integer buyerId, String bookName) {
        // 处理null字符串
        bookName = "null".equals(bookName) ? null : bookName;
        return orderMapper.getOrderInfo(buyerId, bookName);
    }

    
    public List<Order> getOrderSortByTime(Integer buyerId, String bookName) {
        // 处理null字符串
        bookName = "null".equals(bookName) ? null : bookName;
        return orderMapper.getOrderSortByTime(buyerId, bookName);
    }

    
    public List<Order> getOrderSortByStatus(Integer buyerId, String bookName) {
        // 处理null字符串
        bookName = "null".equals(bookName) ? null : bookName;
        return orderMapper.getOrderSortByStatus(buyerId, bookName);
    }

    
    public List<Order> getSoldOrderInfo(Integer sellerId, String bookName) {
        // 处理null字符串
        bookName = "null".equals(bookName) ? null : bookName;
        return orderMapper.getSoldOrderInfo(sellerId, bookName);
    }

    
    public List<Order> getSoldOrderSortByTime(Integer sellerId, String bookName) {
        // 处理null字符串
        bookName = "null".equals(bookName) ? null : bookName;
        return orderMapper.getSoldOrderSortByTime(sellerId, bookName);
    }

    
    public List<Order> getSoldOrderSortByStatus(Integer sellerId, String bookName) {
        // 处理null字符串
        bookName = "null".equals(bookName) ? null : bookName;
        return orderMapper.getSoldOrderSortByStatus(sellerId, bookName);
    }

    
    public List<Order> getAllOrders() {
        return orderMapper.getAllOrders();
    }

    
    public Order getBuyOrderDetail(Integer orderId, Integer buyerId) {
        return orderMapper.getBuyOrderDetail(orderId, buyerId);
    }

    
    public Order getSoldOrderDetail(Integer orderId, Integer sellerId) {
        return orderMapper.getSoldOrderDetail(orderId, sellerId);
    }

    
    public Order getOrderById(Integer orderId) {
        return orderMapper.getOrderById(orderId);
    }

    
    public int updateOrderStatus(Integer orderId, String status) {
        return orderMapper.updateOrderStatus(orderId, status);
    }

    
    @Transactional
    public int refundToBuyer(Integer orderId) {
        try {
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                return -1; // 订单不存在
            }

            // 退款给买家
            int refundResult = orderMapper.addUserBalance(order.getBuyerId(), order.getPrice());
            if (refundResult <= 0) {
                return -2; // 退款失败
            }

            // 更新订单状态为"已退款"并设置完成时间
            int statusResult = orderMapper.updateOrderStatusAndFinishedTime(orderId, "已退款", LocalDateTime.now().toString());
            if (statusResult <= 0) {
                return -3; // 更新状态失败
            }

            // 更新图书状态为已售出
            int bookResult = orderMapper.updateBookStatus(order.getBookId(), 3);
            if (bookResult <= 0) {
                return -4; // 更新图书状态失败
            }

            return 1; // 成功
        } catch (Exception e) {
            throw new RuntimeException("退款操作失败: " + e.getMessage());
        }
    }

    
    @Transactional
    public int rejectRefund(Integer orderId) {
        try {
            Order order = orderMapper.getOrderById(orderId);
            if (order == null) {
                return -1; // 订单不存在
            }

            // 将金额转给卖家
            int transferResult = orderMapper.transferToSeller(order.getSellerId(), order.getPrice());
            if (transferResult <= 0) {
                return -2; // 转账失败
            }

            // 更新订单状态为"拒绝退款"并设置完成时间
            int statusResult = orderMapper.updateOrderStatusAndFinishedTime(orderId, "拒绝退款", LocalDateTime.now().toString());
            if (statusResult <= 0) {
                return -3; // 更新状态失败
            }

            // 更新图书状态为已售出
            int bookResult = orderMapper.updateBookStatus(order.getBookId(), 3);
            if (bookResult <= 0) {
                return -4; // 更新图书状态失败
            }

            return 1; // 成功
        } catch (Exception e) {
            throw new RuntimeException("拒绝退款操作失败: " + e.getMessage());
        }
    }
}