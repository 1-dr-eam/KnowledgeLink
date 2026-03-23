package com.github.common.mq;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息队列dto
 *
 * @author ning
 * @date 2026/03/23
 */
@Data
public class MqMessageDTO {
    private String messageId;
    private String messageBody;
    private LocalDateTime createTime;
}
