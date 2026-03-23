package com.github.common.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class MqProducerDemo {
    private final RabbitTemplate rabbitTemplate;

    public MqProducerDemo(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDemoMessage(MqMessageDTO mqMessageDTO) {
        rabbitTemplate.convertAndSend(MqTopologyConstant.DEMO_EXCHANGE, MqTopologyConstant.DEMO_ROUTING_KEY, mqMessageDTO);
    }
}
