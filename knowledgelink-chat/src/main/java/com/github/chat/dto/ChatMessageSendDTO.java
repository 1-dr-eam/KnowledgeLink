package com.github.chat.dto;

import lombok.Data;

/**
 * 聊天消息发送请求 DTO
 *
 * @author ning
 * @date 2026/03/24
 */
@Data
public class ChatMessageSendDTO {
    private String toUserId;
    private String message;
    private String type;
}
