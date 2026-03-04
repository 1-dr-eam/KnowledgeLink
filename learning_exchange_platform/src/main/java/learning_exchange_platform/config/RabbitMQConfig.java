package learning_exchange_platform.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// A模块：RabbitMQConfig.java（添加常量）
@Configuration
public class RabbitMQConfig {

    // 定义常量，方便B模块使用
    public static final String QUEUE_NAME = "mutual.follow.queue";
    public static final String EXCHANGE_NAME = "mutual.follow.exchange";
    public static final String ROUTING_KEY = "mutual.follow";

    @Bean
    public Queue mutualFollowQueue() {
        return new Queue(QUEUE_NAME, true);  // 使用常量
    }

    @Bean
    public DirectExchange mutualFollowExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false);  // 使用常量
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(mutualFollowQueue())
                .to(mutualFollowExchange())
                .with(ROUTING_KEY);  // 使用常量
    }
}
