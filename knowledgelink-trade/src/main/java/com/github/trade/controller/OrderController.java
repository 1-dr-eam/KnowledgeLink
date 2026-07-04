package com.github.trade.controller;

import com.github.common.dto.Result;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderAddDTO;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.service.IOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器
 *
 * @author ning
 * @date 2026/04/16
 */
@RestController
public class OrderController {

    @Autowired
    private IOrderService orderService;

    /** 查看所有订单信息 */
    @GetMapping("/getAllConsumerOrder")
    public Result getAllConsumerOrder() {
        return orderService.getAllConsumerOrder();
    }

    /** 查看所有售出的订单 */
    @GetMapping("/getAllSellerOrder")
    public Result getAllSellerOrder() {
        return orderService.getAllSellerOrder();
    }

    /** 单本下单 */
    @PostMapping("/addOrder")
    public Result addOrder(@RequestBody OrderAddDTO orderAddDTO) {
        return orderService.addOrder(orderAddDTO);
    }

    /** 批量下单 */
    @PostMapping("/addOrderBatch")
    public Result addOrderBatch(@RequestBody BatchIdRequest batchIdRequest) {
        return orderService.addOrderBatch(batchIdRequest);
    }


    /** 根据id查看对应的订单信息 */
    @GetMapping("/getOrderById")
    public Result getOrderById(@RequestParam("id") String id) {
        IdRequest idRequest = new IdRequest();
        idRequest.setId(id);
        return orderService.getOrderById(idRequest);
    }

    /** 取消订单 */
    @PutMapping("/cancelOrder")
    public Result cancelOrder(@RequestBody IdRequest idRequest) {
        return orderService.cancelOrder(idRequest);
    }

    /** 删除订单 */
    @DeleteMapping("/deleteOrder")
    public Result deleteOrder(@RequestBody IdRequest idRequest) {
        return orderService.deleteOrder(idRequest);
    }

    /** 修改订单状态 */
    @PutMapping("/updateOrderStatus")
    public Result updateOrderStatus(@RequestBody OrderStatusDTO orderStatusDTO) {
        return orderService.updateOrderStatus(orderStatusDTO);
    }
}
