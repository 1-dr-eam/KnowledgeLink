package com.github.chat.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 聊天用户
 *
 * @author ning
 * @date 2026/03/31
 */
@Data
@TableName("user")
public class ChatUser {
    @TableId
    private Long id;
    @TableField("user_name")
    private String username;
    private String phone;
    private String avatar;
    private Integer status;
}
