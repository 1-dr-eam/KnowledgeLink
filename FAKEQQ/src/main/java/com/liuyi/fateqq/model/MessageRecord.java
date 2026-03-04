package com.liuyi.fateqq.model;

import lombok.Data;
import java.util.Date;

//对应消息记录表，用于将消息进行离线存储
@Data
public class MessageRecord {

    private int messageId;   //消息的唯一标识
    private Integer fromUserId;    // 发送消息的用户ID
    private Integer toUserId;      // 接收消息的用户ID
    private String content;     //记录消息内容
    private Date sendTime;          //发送时间
    private Boolean isRead;   //判断是否已读，默认为未读
    private Boolean isImage;

}
