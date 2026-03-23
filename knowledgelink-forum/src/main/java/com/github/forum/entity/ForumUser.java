package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("user")
public class ForumUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String phone;
    private String username;
    private String password;
    private String major;
    private String grade;
    private String avatar;
    private String summary;
    private Double balance;
}
