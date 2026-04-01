package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛实体类
 * 用于存储论坛的详细信息，包括标题、摘要、内容、封面、标签、类型、可见范围、访问量、点赞数、收藏数、评论数等。
 *
 * @author ning
 * @date 2026/03/29
 */
@Data
@TableName("forum")
public class Forum {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("user_id")
    private Long userId;
    private String title;
    private String summary;
    private String content;
    @TableField("cover_avatar")
    private String coverAvatar;
    private String label;
    private String subject;
    @TableField("sub_classify")
    private String subClassify;
    private String type;
    @TableField("visible_range")
    private String visibleRange;
    @TableField("page_views")
    private Integer pageViews;
    @TableField("like_count")
    private Integer likeCount;
    @TableField("collect_count")
    private Integer collectCount;
    @TableField("comment_count")
    private Integer commentCount;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
