package com.github.trade.mq;

import cn.hutool.json.JSONUtil;
import com.github.common.mq.MqMessageDTO;
import com.github.common.mq.MqTopologyConstant;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 订单超时消息生产者
 *
 * @author ning
 * @date 2026/03/23
 */
@Component
public class OrderTimeoutProducer {
    private final RabbitTemplate rabbitTemplate;

    public OrderTimeoutProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * 发送订单超时检查消息
     *
     * @param orderId 订单id
     */
    public void sendOrderTimeoutCheck(Long orderId) {
        Map<String, Object> message = new HashMap<>();
        message.put("orderId", orderId);
        MqMessageDTO mqMessageDTO = new MqMessageDTO();
        mqMessageDTO.setMessageId(UUID.randomUUID().toString());
        mqMessageDTO.setCreateTime(LocalDateTime.now());
        mqMessageDTO.setMessageBody(JSONUtil.toJsonStr(message));
        rabbitTemplate.convertAndSend(MqTopologyConstant.ORDER_TIMEOUT_DELAY_EXCHANGE, MqTopologyConstant.ORDER_TIMEOUT_DELAY_ROUTING_KEY, mqMessageDTO);
    }
}
