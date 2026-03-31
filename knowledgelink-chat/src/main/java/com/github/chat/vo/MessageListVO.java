package com.github.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageListVO {
    private String id;
    private Sender sender;
    private MessageType type;
    private String message;
    private LocalDateTime sendTime;

    public enum Sender {
        ME,
        OPPOSITE
    }

    public enum MessageType {
        TEXT,
        IMAGE
    }
}
