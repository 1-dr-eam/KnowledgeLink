package org.example.book.mapper;

import learning_exchange_platform.model.User;
import org.apache.ibatis.annotations.*;
import org.example.book.entity.Book;
import org.example.book.entity.Order;

import java.util.List;

@Mapper
public interface OrderMapper {

    // 创建订单
    @Insert("INSERT INTO `order` (bookId, bookName, sellerId, buyerId, price, status, createTime, finishedTime, address, deliveryTime) " +
            "VALUES (#{bookId}, #{bookName}, #{sellerId}, #{buyerId}, #{price}, #{status}, #{createTime}, #{finishedTime}, #{address}, #{deliveryTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int createOrder(Order order);

    // 买家取消订单
    @Update("UPDATE `order` SET status = '买家取消交易' WHERE id = #{orderId} AND buyerId = #{buyerId} AND status = '买家已下单'")
    int buyerCancelOrder(@Param("orderId") Integer orderId, @Param("buyerId") Integer buyerId);

    // 更新订单状态和完成时间
    @Update("UPDATE `order` SET status = #{status}, finishedTime = #{finishedTime} WHERE id = #{orderId}")
    int updateOrderStatusAndFinishedTime(@Param("orderId") Integer orderId,
                                         @Param("status") String status,
                                         @Param("finishedTime") String finishedTime);
    // 根据ID获取订单
    @Select("SELECT * FROM `order` WHERE id = #{orderId}")
    Order getOrderById(Integer orderId);

    // 获取所有订单
    @Select("SELECT * FROM `order`")
    List<Order> getAllOrders();

    // 更新订单状态
    @Update("UPDATE `order` SET status = #{status} WHERE id = #{orderId}")
    int updateOrderStatus(@Param("orderId") Integer orderId, @Param("status") String status);

    // 确认收货
    @Update("UPDATE `order` SET status = '交易完成', finishedTime = NOW() WHERE id = #{orderId} AND buyerId = #{buyerId} AND status = '卖家已发货'")
    int confirmReceipt(@Param("orderId") Integer orderId, @Param("buyerId") Integer buyerId);

    // 申请纠纷
    @Update("UPDATE `order` SET status = '交易纠纷中' WHERE id = #{orderId} AND buyerId = #{buyerId} AND status = '卖家已发货'")
    int disputeOrder(@Param("orderId") Integer orderId, @Param("buyerId") Integer buyerId);

    // 获取购买订单
    @Select("SELECT * FROM `order` WHERE buyerId = #{buyerId} AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR bookName LIKE CONCAT('%', #{bookName}, '%')) " +
            "ORDER BY createTime DESC")
    List<Order> getOrderInfo(@Param("buyerId") Integer buyerId, @Param("bookName") String bookName);

    // 购买订单按时间排序
    @Select("SELECT * FROM `order` WHERE buyerId = #{buyerId} AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR bookName LIKE CONCAT('%', #{bookName}, '%')) " +
            "ORDER BY createTime DESC")
    List<Order> getOrderSortByTime(@Param("buyerId") Integer buyerId, @Param("bookName") String bookName);

    // 购买订单按状态排序
    @Select("SELECT * FROM `order` WHERE buyerId = #{buyerId} AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR bookName LIKE CONCAT('%', #{bookName}, '%')) " +
            "ORDER BY status, createTime DESC")
    List<Order> getOrderSortByStatus(@Param("buyerId") Integer buyerId, @Param("bookName") String bookName);

    // 获取售出订单
    @Select("SELECT * FROM `order` WHERE sellerId = #{sellerId} AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR bookName LIKE CONCAT('%', #{bookName}, '%')) " +
            "ORDER BY createTime DESC")
    List<Order> getSoldOrderInfo(@Param("sellerId") Integer sellerId, @Param("bookName") String bookName);

    // 售出订单按时间排序
    @Select("SELECT * FROM `order` WHERE sellerId = #{sellerId} AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR bookName LIKE CONCAT('%', #{bookName}, '%')) " +
            "ORDER BY createTime DESC")
    List<Order> getSoldOrderSortByTime(@Param("sellerId") Integer sellerId, @Param("bookName") String bookName);

    // 售出订单按状态排序
    @Select("SELECT * FROM `order` WHERE sellerId = #{sellerId} AND " +
            "(#{bookName} IS NULL OR #{bookName} = 'null' OR bookName LIKE CONCAT('%', #{bookName}, '%')) " +
            "ORDER BY status, createTime DESC")
    List<Order> getSoldOrderSortByStatus(@Param("sellerId") Integer sellerId, @Param("bookName") String bookName);

    @Select("SELECT * FROM book WHERE id = #{bookId}")
    Book getBookById(Integer bookId);

    @Select("SELECT * FROM user WHERE id = #{userId}")
    User getUserById(Integer userId);

    @Update("UPDATE user SET balance = #{balance} WHERE id = #{userId}")
    int updateUserBalance(@Param("userId") Integer userId, @Param("balance") Double balance);

    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{userId}")
    int addUserBalance(@Param("userId") Integer userId, @Param("amount") Double amount);

    @Update("UPDATE book SET status = #{status} WHERE id = #{bookId}")
    int updateBookStatus(@Param("bookId") Integer bookId, @Param("status") Integer status);

    @Update("UPDATE user SET balance = balance + #{amount} WHERE id = #{sellerId}")
    int transferToSeller(@Param("sellerId") Integer sellerId, @Param("amount") Double amount);

    // 获取购买订单详情
    @Select("SELECT * FROM `order` WHERE id = #{orderId} AND buyerId = #{buyerId}")
    Order getBuyOrderDetail(@Param("orderId") Integer orderId, @Param("buyerId") Integer buyerId);

    // 获取售出订单详情
    @Select("SELECT * FROM `order` WHERE id = #{orderId} AND sellerId = #{sellerId}")
    Order getSoldOrderDetail(@Param("orderId") Integer orderId, @Param("sellerId") Integer sellerId);
}
