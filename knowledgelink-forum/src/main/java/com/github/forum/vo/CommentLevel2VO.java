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
    private Long id;
    private Long userId;
    private String username;
    private String avatar;
    private Long replyUserId;
    private String replayName;
    private String content;
    private Boolean likeStatus;
    private Integer likeCount;
}
