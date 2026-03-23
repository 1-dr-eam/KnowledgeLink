package com.github.chat.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class ChatUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String username;
    private String major;
    private String grade;
    private String avatar;
}
