package com.github.chat.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message_record")
public class ChatMessageRecord {
    @TableId
    private String messageId;
    private Long fromUserId;
    private Long toUserId;
    private String content;
    private LocalDateTime sendTime;
    private Boolean read;
    private Boolean image;
}
