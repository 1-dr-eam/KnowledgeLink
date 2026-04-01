package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户论坛交互实体类
 * 用于记录用户对论坛的交互行为，包括点赞、收藏
 *
 * @author ning
 * @date 2026/03/31
 */
@Data
@TableName("user_forum_interactions")
public class UserForumInteractions {
    // userId_forumId
    @TableId
    @TableField("interaction_id")
    private String interactionId;
    private Long userId;
    private Long forumId;
    private Boolean likeStatus;
    private Boolean collectStatus;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
