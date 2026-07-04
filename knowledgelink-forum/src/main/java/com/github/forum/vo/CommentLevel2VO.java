package com.github.forum.vo;

import lombok.Data;

/**
 * 论坛二级评论
 *
 * @author ning
 * @date 2026/04/01
 */
@Data
public class CommentLevel2VO {
    private String id;
    private String userId;
    private String username;
    private String avatar;
    private String replyUserId;
    private String replyUserName;
    private String content;
    private Boolean likeStatus;
    private Integer likeCount;
}
