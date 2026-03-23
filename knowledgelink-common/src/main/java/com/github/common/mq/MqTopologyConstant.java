package com.github.common.mq;

/**
 * 消息队列常量
 *
 * @author ning
 * @date 2026/03/23
 */
public class MqTopologyConstant {
    public static final String DEMO_EXCHANGE = "knowledgeLink.demo.exchange";
    public static final String DEMO_QUEUE = "knowledgeLink.demo.queue";
    public static final String DEMO_ROUTING_KEY = "knowledgeLink.demo.routingKey";
    public static final String PAY_SUCCESS_EXCHANGE = "knowledgeLink.trade.pay.success.exchange";
    public static final String PAY_SUCCESS_QUEUE = "knowledgeLink.trade.pay.success.queue";
    public static final String PAY_SUCCESS_ROUTING_KEY = "knowledgeLink.trade.pay.success.routingKey";
    public static final String ORDER_TIMEOUT_DELAY_EXCHANGE = "knowledgeLink.trade.order.timeout.delay.exchange";
    public static final String ORDER_TIMEOUT_DELAY_QUEUE = "knowledgeLink.trade.order.timeout.delay.queue";
    public static final String ORDER_TIMEOUT_DELAY_ROUTING_KEY = "knowledgeLink.trade.order.timeout.delay.routingKey";
    public static final String ORDER_TIMEOUT_EXCHANGE = "knowledgeLink.trade.order.timeout.exchange";
    public static final String ORDER_TIMEOUT_QUEUE = "knowledgeLink.trade.order.timeout.queue";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "knowledgeLink.trade.order.timeout.routingKey";
    public static final Integer ORDER_TIMEOUT_TTL_MILLIS = 15 * 60 * 1000;
}
