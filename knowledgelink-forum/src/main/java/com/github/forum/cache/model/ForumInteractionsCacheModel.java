package com.github.forum.cache.model;

import lombok.Data;

/**
 * 论坛互动信息缓存model
 * 用于使用redis缓存存储用户对论坛的互动信息，包括点赞、收藏等。后续同步到数据库UserForumInteractions表
 *
 * @author ning
 * @date 2026/03/31
 */
@Data
public class ForumInteractionsCacheModel {
    // userId_forumId
    private String interactionId;
    private Long userId;
    private Long forumId;
    private Boolean likeStatus;
    private Boolean collectStatus;
}
