package com.github.trade.controller;

import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.service.ISellerOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户作为商户订单controller
 *
 * @author ning
 * @date 2026/03/19
 */
@RestController
public class SellerOrderController {

    @Autowired
    private ISellerOrderService sellerOrderService;

    /**
     * 查看所有售出的订单
     *
     * @return 订单列表
     */
    @GetMapping("/getAllSellerOrder")
    public Result getAllSellerOrder() {
        return sellerOrderService.getAllSellerOrder();
    }

    /**
     * 根据id查看对应的订单信息
     *
     * @param idRequest id请求dto
     * @return 订单信息
     */
    @GetMapping("/getSellerOrderById")
    public Result getSellerOrderById(@RequestBody IdRequest idRequest) {
        return sellerOrderService.getSellerOrderById(idRequest);
    }

    /**
     * 取消订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @PutMapping("/cancelSellerOrder")
    public Result cancelSellerOrder(@RequestBody IdRequest idRequest) {
        return sellerOrderService.cancelSellerOrder(idRequest);
    }

    /**
     * 删除订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @DeleteMapping("/deleteSellerOrder")
    public Result deleteSellerOrder(@RequestBody IdRequest idRequest) {
        return sellerOrderService.deleteSellerOrder(idRequest);
    }

    /**
     * 修改订单状态
     *
     * @param orderStatusDTO 订单状态dto
     * @return success
     */
    @PutMapping("/updateSellerOrderStatus")
    public Result updateSellerOrderStatus(@RequestBody OrderStatusDTO orderStatusDTO) {
        return sellerOrderService.updateSellerOrderStatus(orderStatusDTO);
    }
}
