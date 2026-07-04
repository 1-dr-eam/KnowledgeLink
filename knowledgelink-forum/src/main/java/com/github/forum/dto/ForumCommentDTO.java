package com.github.forum.dto;

import lombok.Data;

/**
 * 论坛评论 dto
 *
 * @author ning
 * @date 2026/04/01
 */
@Data
public class ForumCommentDTO {
    private String forumId;
    // 区分一级评论和二级评论 0表示一级评论， =其他评论的root_id表示属于二级评论
    private String parentId;
    // 聚合同一主楼的所有二级评论 =自身id表示一级评论， =所属的一级评论id
    private String rootId;
    // 回复的评论id 0表示不是回复评论
    private String replyToCommentId;
    // 回复的评论用户昵称 null表示不是回复评论
    private String replyUserName;
    private Integer status;
    private String content;
}
