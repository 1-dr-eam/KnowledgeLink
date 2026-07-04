package com.github.forum.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛帖子 es 文档
 *
 * @author ning
 * @date 2026/04/02
 */
@Data
public class ForumEsDocument {
    private Long id;
    private Long userId;
    private String title;
    private String summary;
    private String content;
    private String coverAvatar;
    private String label;
    private String subject;
    private String subClassify;
    private String type;
    private String visibleRange;
    private Integer pageViews;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
