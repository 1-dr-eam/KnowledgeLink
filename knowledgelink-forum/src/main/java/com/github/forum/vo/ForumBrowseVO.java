package com.github.forum.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 论坛浏览vo
 * 用于首页简要展示帖子信息
 *
 * @author ning
 * @date 2026/04/01
 */
@Data
public class ForumBrowseVO {
    private Long id;
    private Long userId;
    private String userName;
    private String userAvatar;
    private String title;
    private String summary;
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
}
