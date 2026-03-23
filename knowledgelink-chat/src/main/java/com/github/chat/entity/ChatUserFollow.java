package com.github.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_user_follow")
public class ChatUserFollow {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long followUserId;
    private LocalDateTime createTime;
}
