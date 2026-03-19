package com.github.trade.controller;

import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderAddDTO;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.service.IConsumerOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户作为消费者订单controller
 *
 * @author ning
 * @date 2026/03/19
 */
@RestController
public class ConsumerOrderController {

    @Autowired
    private IConsumerOrderService consumerOrderService;

    /**
     * 添加订单
     *
     * @param orderAddDTO 下单请求dto
     * @return success
     */
    @PostMapping("/addConsumerOrder")
    public Result addConsumerOrder(@RequestBody OrderAddDTO orderAddDTO) {
        return consumerOrderService.addConsumerOrder(orderAddDTO);
    }

    /**
     * 取消订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @PutMapping("/cancelConsumerOrder")
    public Result cancelConsumerOrder(@RequestBody IdRequest idRequest) {
        return consumerOrderService.cancelConsumerOrder(idRequest);
    }

    /**
     * 查看所有订单信息
     *
     * @return 订单列表
     */
    @GetMapping("/getAllConsumerOrder")
    public Result getAllConsumerOrder() {
        return consumerOrderService.getAllConsumerOrder();
    }

    /**
     * 根据id查看订单信息
     *
     * @param idRequest id请求dto
     * @return 订单信息
     */
    @GetMapping("/getConsumerOrderById")
    public Result getConsumerOrderById(@RequestBody IdRequest idRequest) {
        return consumerOrderService.getConsumerOrderById(idRequest);
    }

    /**
     * 删除订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @DeleteMapping("/deleteConsumerOrder")
    public Result deleteConsumerOrder(@RequestBody IdRequest idRequest) {
        return consumerOrderService.deleteConsumerOrder(idRequest);
    }

    /**
     * 修改订单状态
     *
     * @param orderStatusDTO 订单状态dto
     * @return success
     */
    @PutMapping("/updateConsumerOrderStatus")
    public Result updateConsumerOrderStatus(@RequestBody OrderStatusDTO orderStatusDTO) {
        return consumerOrderService.updateConsumerOrderStatus(orderStatusDTO);
    }
}
