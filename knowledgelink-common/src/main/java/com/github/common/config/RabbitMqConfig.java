package com.github.common.config;

import com.github.common.mq.MqTopologyConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * rabbitMQ配置
 *
 * @author ning
 * @date 2026/03/23
 */
@Configuration
public class RabbitMqConfig {
    @Bean
    public TopicExchange demoExchange() {
        return new TopicExchange(MqTopologyConstant.DEMO_EXCHANGE, true, false);
    }

    @Bean
    public Queue demoQueue() {
        return new Queue(MqTopologyConstant.DEMO_QUEUE, true, false, false);
    }

    @Bean
    public Binding demoBinding(@Qualifier("demoQueue") Queue demoQueue, @Qualifier("demoExchange") TopicExchange demoExchange) {
        return BindingBuilder.bind(demoQueue).to(demoExchange).with(MqTopologyConstant.DEMO_ROUTING_KEY);
    }

    @Bean
    public TopicExchange paySuccessExchange() {
        return new TopicExchange(MqTopologyConstant.PAY_SUCCESS_EXCHANGE, true, false);
    }

    @Bean
    public Queue paySuccessQueue() {
        return new Queue(MqTopologyConstant.PAY_SUCCESS_QUEUE, true, false, false);
    }

    @Bean
    public Binding paySuccessBinding(@Qualifier("paySuccessQueue") Queue paySuccessQueue, @Qualifier("paySuccessExchange") TopicExchange paySuccessExchange) {
        return BindingBuilder.bind(paySuccessQueue).to(paySuccessExchange).with(MqTopologyConstant.PAY_SUCCESS_ROUTING_KEY);
    }

    @Bean
    public TopicExchange orderTimeoutDelayExchange() {
        return new TopicExchange(MqTopologyConstant.ORDER_TIMEOUT_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderTimeoutDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqTopologyConstant.ORDER_TIMEOUT_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqTopologyConstant.ORDER_TIMEOUT_ROUTING_KEY);
        args.put("x-message-ttl", MqTopologyConstant.ORDER_TIMEOUT_TTL_MILLIS);
        return new Queue(MqTopologyConstant.ORDER_TIMEOUT_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding orderTimeoutDelayBinding(@Qualifier("orderTimeoutDelayQueue") Queue orderTimeoutDelayQueue, @Qualifier("orderTimeoutDelayExchange") TopicExchange orderTimeoutDelayExchange) {
        return BindingBuilder.bind(orderTimeoutDelayQueue).to(orderTimeoutDelayExchange).with(MqTopologyConstant.ORDER_TIMEOUT_DELAY_ROUTING_KEY);
    }

    @Bean
    public TopicExchange orderTimeoutExchange() {
        return new TopicExchange(MqTopologyConstant.ORDER_TIMEOUT_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return new Queue(MqTopologyConstant.ORDER_TIMEOUT_QUEUE, true, false, false);
    }

    @Bean
    public Binding orderTimeoutBinding(@Qualifier("orderTimeoutQueue") Queue orderTimeoutQueue, @Qualifier("orderTimeoutExchange") TopicExchange orderTimeoutExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderTimeoutExchange).with(MqTopologyConstant.ORDER_TIMEOUT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
