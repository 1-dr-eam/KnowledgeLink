package com.github.trade.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.github.common.mq.MqMessageDTO;
import com.github.common.mq.MqTopologyConstant;
import com.github.trade.entity.Order;
import com.github.trade.mapper.ConsumerOrderMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

import static com.github.trade.util.RedisConstant.ORDER_KEY;
import static com.github.trade.util.RedisConstant.ORDER_TTL;

/**
 * 微信支付成功消息消费者
 *
 * @author ning
 * @date 2026/03/23
 */
@Component
public class WechatPaySuccessConsumer {
    private static final String ORDER_STATUS_WAIT_PAY = "待支付";
    private static final String ORDER_STATUS_WAIT_SHIP = "待发货";
    private final ConsumerOrderMapper consumerOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public WechatPaySuccessConsumer(ConsumerOrderMapper consumerOrderMapper, StringRedisTemplate stringRedisTemplate) {
        this.consumerOrderMapper = consumerOrderMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 消费支付成功消息并更新订单状态
     *
     * @param mqMessageDTO 支付成功消息体
     */
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = MqTopologyConstant.PAY_SUCCESS_QUEUE)
    public void consumePaySuccessMessage(MqMessageDTO mqMessageDTO) {
        if (mqMessageDTO == null || StrUtil.isBlank(mqMessageDTO.getMessageBody())) {
            return;
        }
        JSONObject jsonObject = JSONUtil.parseObj(mqMessageDTO.getMessageBody());
        Long orderId = jsonObject.getLong("orderId");
        if (orderId == null) {
            return;
        }
        String targetStatus = jsonObject.getStr("targetStatus");
        if (StrUtil.isBlank(targetStatus)) {
            targetStatus = ORDER_STATUS_WAIT_SHIP;
        }
        Order order = consumerOrderMapper.selectById(orderId);
        if (order == null || !ORDER_STATUS_WAIT_PAY.equals(order.getStatus())) {
            return;
        }
        order.setStatus(targetStatus);
        consumerOrderMapper.updateById(order);
        String orderJson = JSONUtil.toJsonStr(order);
        stringRedisTemplate.opsForValue().set(ORDER_KEY + orderId, orderJson, ORDER_TTL, TimeUnit.MINUTES);
    }
}
