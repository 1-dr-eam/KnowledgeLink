package com.github.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderAddDTO;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Order;

/**
 * 用户作为消费者订单service接口
 *
 * @author ning
 * @date 2026/03/19
 */
public interface IConsumerOrderService extends IService<Order> {
    Result addConsumerOrder(OrderAddDTO orderAddDTO);

    Result cancelConsumerOrder(IdRequest idRequest);

    Result getAllConsumerOrder();

    Result getConsumerOrderById(IdRequest idRequest);

    Result deleteConsumerOrder(IdRequest idRequest);

    Result updateConsumerOrderStatus(OrderStatusDTO orderStatusDTO);
}
