package com.liuyi.fateqq.websocket.model;

import lombok.Data;


//前端发送给服务器的消息的消息
@Data
public class Message {

    private String toUserId;      // 接收者用户ID(好友ID)
    private String message;        // 消息内容

}