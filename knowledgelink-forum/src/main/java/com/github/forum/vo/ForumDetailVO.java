package com.github.forum.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子vo
 * 用于传递帖子的详细信息，包括帖子的基本信息、用户信息、点赞状态、收藏状态等。
 *
 * @author ning
 * @date 2026/03/31
 */
@Data
public class ForumDetailVO {
    private String id;
    private String userId;
    private String userName;
    private String userAvatar;
    // 是否是自己的帖子（若是则followStatus为null）
    private Boolean selfForum;
    private Boolean followStatus;
    private Boolean likeStatus;
    private Boolean collectStatus;
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
}
