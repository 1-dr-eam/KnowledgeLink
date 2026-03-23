package com.github.chat.dto;

import lombok.Data;

@Data
public class ChatSendMessageDTO {
    private Long toUserId;
    private String message;
    private String type;
}
