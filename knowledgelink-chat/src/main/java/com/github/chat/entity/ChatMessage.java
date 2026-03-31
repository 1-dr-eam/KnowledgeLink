package com.github.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息记录实体
 *
 * @author ning
 * @date 2026/03/24
 */
@Data
@TableName("chat_message")
public class ChatMessage {
    // 使用组合索引 -> Aid_Bid
    @TableId
    private String id;
    @TableField("sender_id")
    private Long senderId;
    @TableField("receiver_id")
    private Long receiverId;
    @TableField("message_type")
    private MessageType messageType;
    private String message;
    @TableField("send_time")
    private LocalDateTime sendTime;
    private Boolean read;

    public enum MessageType {
        TEXT, IMAGE
    }
}
