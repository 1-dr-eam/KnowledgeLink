package com.github.trade.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.trade.dto.BookDTO;
import com.github.trade.dto.IdRequest;
import com.github.trade.dto.OrderAddDTO;
import com.github.trade.dto.OrderStatusDTO;
import com.github.trade.entity.Order;
import com.github.trade.mapper.ConsumerOrderMapper;
import com.github.trade.service.IConsumerOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.github.trade.util.RedisConstant.ORDER_KEY;
import static com.github.trade.util.RedisConstant.ORDER_TTL;

/**
 * 用户作为消费者订单service实现类
 *
 * @author ning
 * @date 2026/03/19
 */
@Service
public class ConsumerOrderServiceImpl extends ServiceImpl<ConsumerOrderMapper, Order> implements IConsumerOrderService {

    @Autowired
    private BookServiceImpl bookServiceImpl;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 添加订单
     *
     * @param orderAddDTO 下单请求dto
     * @return success
     */
    @Override
    public Result addConsumerOrder(OrderAddDTO orderAddDTO) {
        BookDTO bookDTO = bookServiceImpl.getBookInfoById(orderAddDTO.getBookId());
        if(bookDTO == null){
            return Result.error("商品信息不存在请重试");
        }
        if(bookDTO.getCount() < orderAddDTO.getCount()) {
            return Result.error("商品库存不足");
        }
        Order order = new Order();
        order.setUserId(UserHolder.getUser().getId());
        order.setSellerId(bookDTO.getSellerId());
        order.setBookId(orderAddDTO.getBookId());
        order.setPrice(bookDTO.getPrice());
        order.setCount(orderAddDTO.getCount());
        order.setTotalPrice(bookDTO.getPrice() * orderAddDTO.getCount());
        order.setBookName(bookDTO.getName());
        order.setStatus("待支付");
        order.setAddress(orderAddDTO.getAddress());
        baseMapper.insert(order);
        String tokenKey = ORDER_KEY + order.getId();
        String orderJson = JSONUtil.toJsonStr(order);
        stringRedisTemplate.opsForValue().set(tokenKey, orderJson, ORDER_TTL, TimeUnit.MINUTES);
        // TODO 暂且使用旁路缓存，后期讨论是否需要设置分布式锁


        return Result.success();
    }

    /**
     * 取消订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @Override
    public Result cancelConsumerOrder(IdRequest idRequest) {
        return Result.success();
    }

    /**
     * 查看所有订单信息
     *
     * @return 订单列表
     */
    @Override
    public Result getAllConsumerOrder() {
        return Result.success();
    }

    /**
     * 根据id查看订单信息
     *
     * @param idRequest id请求dto
     * @return 订单信息
     */
    @Override
    public Result getConsumerOrderById(IdRequest idRequest) {
        return Result.success();
    }

    /**
     * 删除订单
     *
     * @param idRequest id请求dto
     * @return success
     */
    @Override
    public Result deleteConsumerOrder(IdRequest idRequest) {
        return Result.success();
    }

    /**
     * 修改订单状态
     *
     * @param orderStatusDTO 订单状态dto
     * @return success
     */
    @Override
    public Result updateConsumerOrderStatus(OrderStatusDTO orderStatusDTO) {
        return Result.success();
    }
}
