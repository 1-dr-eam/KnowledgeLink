package com.github.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 *
 * @author ning
 * @date 2026/03/25
 */
@Data
@TableName("user")
public class User {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String phone;
    @TableField("user_name")
    private String username;
    private String password;
    private String major;
    private String grade;
    private String avatar;
    private String summary;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time",  fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    private Integer status;
}
