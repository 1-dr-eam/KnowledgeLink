package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛评论点赞实体类
 * 用于记录用户对论坛评论的点赞行为，包括点赞用户、点赞评论、点赞时间等。
 *
 * @author ning
 * @date 2026/03/31
 */
@Data
@TableName("forum_comment_like")
public class ForumCommentLike {
    // userId_commentId
    @TableId
    private Long id;
    @TableField("forum_id")
    private Long forumId;
    @TableField("user_id")
    private Long userId;
    @TableField("comment_id")
    private Long commentId;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
