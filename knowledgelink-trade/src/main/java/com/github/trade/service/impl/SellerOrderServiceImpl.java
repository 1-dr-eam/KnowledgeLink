package com.github.trade.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Order;
import com.github.trade.mapper.SellerOrderMapper;
import com.github.trade.service.ISellerOrderService;
import org.springframework.stereotype.Service;

/**
 * 用户作为商户订单service实现类
 *
 * @author ning
 * @date 2026/03/19
 */
@Service
public class SellerOrderServiceImpl extends ServiceImpl<SellerOrderMapper, Order> implements ISellerOrderService {
    /**
     * 查看所有售出的订单
     *
     * @return 订单列表
     */
    @Override
    public Result getAllSellerOrder() {
        return Result.success();
    }

    /**
     * 根据id查看对应的订单信息
     *
     * @param idRequest id请求dto
     * @return 订单信息
     */
    @Override
    public Result getSellerOrderById(IdRequest idRequest) {
        return Result.success();
    }

    /**
     * 取消订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @Override
    public Result cancelSellerOrder(IdRequest idRequest) {
        return Result.success();
    }

    /**
     * 删除订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @Override
    public Result deleteSellerOrder(IdRequest idRequest) {
        return Result.success();
    }

    /**
     * 修改订单状态
     *
     * @param orderStatusDTO 订单状态dto
     * @return success
     */
    @Override
    public Result updateSellerOrderStatus(OrderStatusDTO orderStatusDTO) {
        return Result.success();
    }
}
