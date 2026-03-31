package com.github.trade.mq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.common.mq.MqMessageDTO;
import com.github.common.mq.MqTopologyConstant;
import com.github.trade.entity.Book;
import com.github.trade.entity.Order;
import com.github.trade.mapper.BookMapper;
import com.github.trade.mapper.ConsumerOrderMapper;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.github.common.utils.RedisConstant.BOOK_INFO_KEY;
import static com.github.common.utils.RedisConstant.BOOK_INFO_TTL;
import static com.github.common.utils.RedisConstant.ORDER_KEY;
import static com.github.common.utils.RedisConstant.ORDER_TTL;

/**
 * 订单超时消息消费者
 *
 * @author ning
 * @date 2026/03/23
 */
@Component
public class OrderTimeoutConsumer {
    private static final String ORDER_STATUS_WAIT_PAY = "待支付";
    private static final String ORDER_STATUS_TIMEOUT = "订单超时";
    private final ConsumerOrderMapper consumerOrderMapper;
    private final BookMapper bookMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public OrderTimeoutConsumer(ConsumerOrderMapper consumerOrderMapper, BookMapper bookMapper, StringRedisTemplate stringRedisTemplate) {
        this.consumerOrderMapper = consumerOrderMapper;
        this.bookMapper = bookMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 消费订单超时消息并自动关单
     *
     * @param mqMessageDTO 超时消息体
     */
    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = MqTopologyConstant.ORDER_TIMEOUT_QUEUE)
    public void consumeOrderTimeoutMessage(MqMessageDTO mqMessageDTO) {
        if (mqMessageDTO == null || StrUtil.isBlank(mqMessageDTO.getMessageBody())) {
            return;
        }
        JSONObject jsonObject = JSONUtil.parseObj(mqMessageDTO.getMessageBody());
        Long orderId = jsonObject.getLong("orderId");
        if (orderId == null) {
            return;
        }
        Order order = consumerOrderMapper.selectById(orderId);
        if (order == null) {
            return;
        }
        int affectRows = consumerOrderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .eq(Order::getId, orderId)
                .eq(Order::getStatus, ORDER_STATUS_WAIT_PAY)
                .set(Order::getStatus, ORDER_STATUS_TIMEOUT)
                .set(Order::getFinishedTime, LocalDateTime.now()));
        if (affectRows <= 0) {
            return;
        }
        // 库存回填
        if (order.getBookId() != null && order.getCount() != null && order.getCount() > 0) {
            bookMapper.update(null, new LambdaUpdateWrapper<Book>()
                    .eq(Book::getItemId, order.getBookId())
                    .setSql("count = count + " + order.getCount()));
            Book latestBook = bookMapper.selectById(order.getBookId());
            if (latestBook != null) {
                stringRedisTemplate.opsForValue().set(BOOK_INFO_KEY + latestBook.getItemId(), JSONUtil.toJsonStr(latestBook), BOOK_INFO_TTL, TimeUnit.MINUTES);
            }
        }
        order.setStatus(ORDER_STATUS_TIMEOUT);
        order.setFinishedTime(LocalDateTime.now());
        stringRedisTemplate.opsForValue().set(ORDER_KEY + orderId, JSONUtil.toJsonStr(order), ORDER_TTL, TimeUnit.MINUTES);
    }
}
