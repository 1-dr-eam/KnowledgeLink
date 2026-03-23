package com.github.common.mq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MqConsumerDemo {
    @RabbitListener(queues = MqTopologyConstant.DEMO_QUEUE)
    public void consumeDemoMessage(MqMessageDTO mqMessageDTO) {
    }
}
