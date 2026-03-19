package com.github.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Order;

/**
 * 用户作为商户订单service接口
 *
 * @author ning
 * @date 2026/03/19
 */
public interface ISellerOrderService extends IService<Order> {
    Result getAllSellerOrder();

    Result getSellerOrderById(IdRequest idRequest);

    Result cancelSellerOrder(IdRequest idRequest);

    Result deleteSellerOrder(IdRequest idRequest);

    Result updateSellerOrderStatus(OrderStatusDTO orderStatusDTO);
}
