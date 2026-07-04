package com.github.trade.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.common.dto.Result;
import com.github.trade.dto.BatchIdRequest;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderAddDTO;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Order;

public interface IOrderService extends IService<Order> {
    Result addOrder(OrderAddDTO orderAddDTO);

    Result addOrderBatch(BatchIdRequest batchIdRequest);

    Result getOrderById(IdRequest idRequest);

    Result cancelOrder(IdRequest idRequest);

    Result deleteOrder(IdRequest idRequest);

    Result getAllConsumerOrder();

    Result getAllSellerOrder();

    Result updateOrderStatus(OrderStatusDTO orderStatusDTO);
}
