package com.github.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注实体类
 *
 * @author ning
 * @date 2026/03/25
 */
@Data
@TableName("user_follow")
public class UserFollow {
    @TableId
    private String id;
    @TableField("first_user_id")
    private Long firstUserId;
    @TableField("second_user_id")
    private Long secondUserId;
    private Status status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    public enum Status{
        FIRST_FOLLOW,
        SECOND_FOLLOW,
        EACH_FOLLOW
    }
}
