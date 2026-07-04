package com.github.forum.vo;

import lombok.Data;

/**
 * 论坛一级评论
 *
 * @author ning
 * @date 2026/04/01
 */
@Data
public class CommentLevel1VO {
    private String id;
    private String userId;
    private String username;
    private String avatar;
    private String content;
    private Boolean likeStatus;
    private Integer likeCount;
    private Integer commentCount;
}
