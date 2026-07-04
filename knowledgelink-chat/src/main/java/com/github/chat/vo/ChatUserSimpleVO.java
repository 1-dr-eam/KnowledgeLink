package com.github.chat.vo;

import lombok.Data;

@Data
public class ChatUserSimpleVO {
    private String id;
    private String username;
    private String phone;
    private String avatar;
    private Integer status;
}
