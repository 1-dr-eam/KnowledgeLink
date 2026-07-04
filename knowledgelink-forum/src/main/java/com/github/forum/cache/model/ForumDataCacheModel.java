package com.github.forum.cache.model;

import lombok.Data;

/**
 * 论坛数据缓存model
 * 用于存储论坛的详细信息，包括论坛ID、页面访问量、点赞数、收藏数、评论数等。
 * 作为Forum的redis缓存类，后续定时任务向MySQL同步数据
 *
 * @author ning
 * @date 2026/03/31
 */
@Data
public class ForumDataCacheModel {
    private Long forumId;
    private Integer pageViews;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
}
