package com.liuyi.fateqq.controller;

import com.liuyi.fateqq.service.ChatOrderService;
import learning_exchange_platform.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/orders")
public class ChatOrderController {

    @Autowired
    private ChatOrderService chatOrderService;

    //GET /api/orders/1203,"1203"就是订单Id
    @GetMapping("/{orderId}")
    public Result getOrderById(@PathVariable Integer orderId) {
        try {
            System.out.println("收到订单查询请求，订单ID: " + orderId);
            //返回订单信息
            Integer order = chatOrderService.getOrderById(orderId);

            System.out.println("成功查询到订单: " + order);
            Map<String, Object> map = new HashMap<>();
            map.put("type", "order");
            map.put("order", order);
            return Result.success(map);

        } catch (Exception e) {
            System.out.println("查询订单异常: " + e.getMessage());
            return null;
        }
    }
}