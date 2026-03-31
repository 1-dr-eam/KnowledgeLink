package com.github.chat.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatListVO {
    private Long id;
    private String avatar;
    private String name;
    private Boolean status;
    private String lastMessage;
    private Boolean read;
    private LocalDateTime sendTime;
}
