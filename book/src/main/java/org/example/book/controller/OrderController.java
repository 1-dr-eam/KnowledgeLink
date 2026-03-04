package org.example.book.controller;

import io.swagger.v3.oas.annotations.Operation;
import learning_exchange_platform.model.User;
import org.example.book.entity.Book;
import org.example.book.entity.Order;
import org.example.book.entity.Result;
import org.example.book.service.BookService;
import org.example.book.service.OrderService;
import org.example.book.util.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private BookService bookService;

    @Autowired
    private SessionManager sessionManager;

    @Operation(summary = "购买图书")
    @PostMapping("/purchase")
    public Result purchaseBook(@RequestParam Integer bookId, @RequestParam Integer buyerId) {
        try {
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(buyerId)) {
                return Result.error("无权操作其他用户的订单");
            }

            int result = orderService.purchaseBook(bookId, buyerId);
            if (result > 0) {
                return Result.success("购买成功");
            } else if (result == -1) {
                return Result.error("图书不存在");
            } else if (result == -2) {
                return Result.error("图书已下架或已被购买");
            } else if (result == -3) {
                return Result.error("买家不存在");
            } else if (result == -4) {
                return Result.error("余额不足");
            } else if (result == -5) {
                return Result.error("不能购买自己发布的图书");
            } else {
                return Result.error("购买失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "买家取消订单")
    @PutMapping("/buyer/cancel")
    public Result buyerCancelOrder(@RequestParam Integer orderId, @RequestParam Integer buyerId) {
        try {
            // 验证当前用户是否与buyerId匹配
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(buyerId)) {
                return Result.error("无权操作其他用户的订单");
            }

            int result = orderService.buyerCancelOrder(orderId, buyerId);
            if (result > 0) {
                return Result.success("买家取消订单成功，图书已恢复上架，余额已退还");
            } else if (result == -1) {
                return Result.error("订单不存在");
            } else if (result == -2) {
                return Result.error("订单不属于该买家");
            } else if (result == -3) {
                return Result.error("订单状态不允许取消，只有'买家已下单'状态的订单才能被买家取消");
            } else if (result == -4) {
                return Result.error("恢复买家余额失败");
            } else if (result == -5) {
                return Result.error("更新图书状态失败");
            } else if (result == -6) {
                return Result.error("更新订单状态失败");
            } else {
                return Result.error("订单取消失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "卖家发货")
    @PutMapping("/ship")
    public Result shipOrder(@RequestParam Integer orderId) {
        try {
            int result = orderService.shipOrder(orderId);
            if (result > 0) {
                return Result.success("发货成功");
            } else {
                return Result.error("发货失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "卖家取消订单")
    @PutMapping("/cancel")
    public Result cancelOrder(@RequestParam Integer orderId,
                              @RequestParam Integer bookId,
                              @RequestParam Integer sellerId) {
        try {
            // 验证当前用户是否与sellerId匹配
            if (!sessionManager.isCurrentUser(sellerId)) {
                return Result.error("无权操作其他用户的订单");
            }

            int result = orderService.cancelOrder(orderId, bookId, sellerId);
            if (result > 0) {
                return Result.success("卖家取消交易，图书已恢复上架，买家余额已退还");
            } else if (result == -1) {
                return Result.error("订单不存在");
            } else if (result == -2) {
                return Result.error("订单不属于该卖家");
            } else if (result == -3) {
                return Result.error("订单与图书不匹配");
            } else if (result == -4) {
                return Result.error("订单状态不允许取消，只有'买家已下单'状态的订单才能被卖家取消");
            } else if (result == -5) {
                return Result.error("恢复买家余额失败");
            } else if (result == -6) {
                return Result.error("更新图书状态失败");
            } else if (result == -7) {
                return Result.error("更新订单状态失败");
            } else {
                return Result.error("订单取消失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "买家确认收货")
    @PutMapping("/confirm")
    public Result confirmReceipt(@RequestParam Integer orderId, @RequestParam Integer buyerId) {
        try {
            // 验证当前用户是否与buyerId匹配 - 手动验证
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(buyerId)) {
                return Result.error("无权操作其他用户的订单");
            }

            int result = orderService.confirmReceipt(orderId, buyerId);
            if (result > 0) {
                return Result.success("确认收货成功，交易完成，书款已转给卖家");
            } else if (result == -1) {
                return Result.error("订单不存在");
            } else if (result == -2) {
                return Result.error("订单不属于该买家");
            } else if (result == -3) {
                return Result.error("订单状态不允许确认收货，只有'卖家已发货'状态的订单才能确认收货");
            } else if (result == -4) {
                return Result.error("确认收货失败");
            } else if (result == -5) {
                return Result.error("转账给卖家失败");
            } else if (result == -6) {
                return Result.error("更新图书状态失败");
            } else {
                return Result.error("确认收货失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "申请纠纷介入")
    @PutMapping("/dispute")
    public Result disputeOrder(@RequestParam Integer orderId, @RequestParam Integer buyerId) {
        try {
            // 验证当前用户是否与buyerId匹配
            if (!sessionManager.isCurrentUser(buyerId)) {
                return Result.error("无权操作其他用户的订单");
            }

            int result = orderService.disputeOrder(orderId, buyerId);
            if (result > 0) {
                return Result.success("申请纠纷介入成功，客服将尽快处理");
            } else if (result == -1) {
                return Result.error("订单不存在");
            } else if (result == -2) {
                return Result.error("订单不属于该买家");
            } else if (result == -3) {
                return Result.error("订单状态不允许申请纠纷，只有'卖家已发货'状态的订单才能申请纠纷");
            } else if (result == -4) {
                return Result.error("申请纠纷介入失败");
            } else {
                return Result.error("申请纠纷介入失败");
            }
        } catch (Exception e) {
            return Result.error("发生错误: " + e.getMessage());
        }
    }

    @Operation(summary = "获取购买订单")
    @GetMapping("/buyer")
    public Result getOrderInfo(@RequestParam Integer buyerId,
                               @RequestParam(required = false) String bookName) {
        try {
            // 验证当前用户是否与buyerId匹配 - 手动验证
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(buyerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;

            List<Order> orders = orderService.getOrderInfo(buyerId, bookName);
            return Result.success(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取订单信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "购买订单按时间排序")
    @GetMapping("/buyer/sort-by-time")
    public Result getOrderSortByTime(@RequestParam Integer buyerId,
                                     @RequestParam(required = false) String bookName) {
        try {
            // 验证当前用户是否与buyerId匹配
            if (!sessionManager.isCurrentUser(buyerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;

            List<Order> orders = orderService.getOrderSortByTime(buyerId, bookName);
            return Result.success(orders);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取订单信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "购买订单按状态排序")
    @GetMapping("/buyer/sort-by-status")
    public Result getOrderSortByStatus(@RequestParam Integer buyerId,
                                       @RequestParam(required = false) String bookName) {
        try {
            // 验证当前用户是否与buyerId匹配
            if (!sessionManager.isCurrentUser(buyerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;

            List<Order> orders = orderService.getOrderSortByStatus(buyerId, bookName);
            if (!orders.isEmpty()) {
                return Result.success(orders);
            } else {
                return Result.error("没有找到相关订单");
            }
        } catch (Exception e) {
            return Result.error("获取订单信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取售出订单")
    @GetMapping("/seller")
    public Result getSoldOrderInfo(@RequestParam Integer sellerId,
                                   @RequestParam(required = false) String bookName) {
        try {
            // 验证当前用户是否与sellerId匹配
            if (!sessionManager.isCurrentUser(sellerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;

            List<Order> orders = orderService.getSoldOrderInfo(sellerId, bookName);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取售出订单信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "售出订单按时间排序")
    @GetMapping("/seller/sort-by-time")
    public Result getSoldOrderSortByTime(@RequestParam Integer sellerId,
                                         @RequestParam(required = false) String bookName) {
        try {
            // 验证当前用户是否与sellerId匹配
            if (!sessionManager.isCurrentUser(sellerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;

            List<Order> orders = orderService.getSoldOrderSortByTime(sellerId, bookName);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取售出订单信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "售出订单按状态排序")
    @GetMapping("/seller/sort-by-status")
    public Result getSoldOrderSortByStatus(@RequestParam Integer sellerId,
                                           @RequestParam(required = false) String bookName) {
        try {
            // 验证当前用户是否与sellerId匹配
            if (!sessionManager.isCurrentUser(sellerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            // 处理null字符串
            bookName = "null".equals(bookName) ? null : bookName;

            List<Order> orders = orderService.getSoldOrderSortByStatus(sellerId, bookName);
            return Result.success(orders);
        } catch (Exception e) {
            return Result.error("获取售出订单信息失败: " + e.getMessage());
        }
    }



    @Operation(summary = "获取购买订单详情")
    @GetMapping("/buyer/detail")
    public Result getBuyOrderDetail(@RequestParam Integer orderId, @RequestParam Integer buyerId) {
        try {
            // 验证当前用户是否与buyerId匹配
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(buyerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            Order order = orderService.getBuyOrderDetail(orderId, buyerId);
            if (order != null) {
                return Result.success(order);
            } else {
                return Result.error("订单不存在");
            }
        } catch (Exception e) {
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取售出订单详情")
    @GetMapping("/seller/detail")
    public Result getSoldOrderDetail(@RequestParam Integer orderId, @RequestParam Integer sellerId) {
        try {
            // 验证当前用户是否与sellerId匹配
            User currentUser = sessionManager.getCurrentUser();
            if (currentUser == null || !currentUser.getId().equals(sellerId)) {
                return Result.error("无权查看其他用户的订单");
            }

            Order order = orderService.getSoldOrderDetail(orderId, sellerId);
            if (order != null) {
                return Result.success(order);
            } else {
                return Result.error("订单不存在");
            }
        } catch (Exception e) {
            return Result.error("获取订单详情失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据书籍ID获取书籍详情")
    @GetMapping("/book/detail")
    public Result getBookInfoById(@RequestParam Integer bookId) {
        try {
            // 调用BookService获取书籍详情
            List<Book> books = bookService.getBookInfoById(bookId);
            if (books != null && !books.isEmpty()) {
                return Result.success(books.get(0)); // 返回第一个匹配的书籍
            } else {
                return Result.error("书籍不存在");
            }
        } catch (Exception e) {
            return Result.error("获取书籍信息失败: " + e.getMessage());
        }
    }

    @Operation(summary = "根据书籍ID获取书籍详情（订单专用，不限制状态）")
    @GetMapping("/book/order-detail")
    public Result getBookInfoForOrder(@RequestParam Integer bookId) {
        try {
            // 使用新的方法，不限制状态
            List<Book> books = bookService.getBookByIdForOrder(bookId);
            if (books != null && !books.isEmpty()) {
                return Result.success(books.get(0));
            } else {
                return Result.error("书籍不存在");
            }
        } catch (Exception e) {
            return Result.error("获取书籍信息失败: " + e.getMessage());
        }
    }
}