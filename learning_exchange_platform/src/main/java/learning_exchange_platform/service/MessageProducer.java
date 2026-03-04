package learning_exchange_platform.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class MessageProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendMutualFollowEvent(Integer user1Id, Integer user2Id) {
        // 构建消息
        Map<String, Object> message = new HashMap<>();
        message.put("eventId", UUID.randomUUID().toString());
        message.put("eventType", "USER_MUTUAL_FOLLOW");
        message.put("user1Id", user1Id);
        message.put("user2Id", user2Id);
        message.put("timestamp", new Date());

        // 发送消息
        rabbitTemplate.convertAndSend(
                "mutual.follow.exchange",
                "mutual.follow",
                message
        );

        log.info("MQ消息发送成功: 用户{}和用户{}互相关注", user1Id, user2Id);
    }
}
