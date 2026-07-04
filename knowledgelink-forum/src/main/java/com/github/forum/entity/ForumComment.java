package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * ForumComment 实体类
 * 用于存储论坛评论的详细信息，包括评论内容、评论状态、点赞数、评论时间等。
 * 评论可以是一级评论或二级评论，一级评论是论坛的根评论，二级评论是回复一级评论的评论。
 * 评论状态包括待审核、已审核、已删除等。（后续开发）
 *
 * @author ning
 * @date 2026/03/24
 */

@Data
@TableName("forum_comment")
public class ForumComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("forum_id")
    private Long forumId;
    @TableField("user_id")
    private Long userId;
    // 区分一级评论和二级评论 0表示一级评论， =其他评论的root_id表示属于二级评论
    @TableField("parent_id")
    private Long parentId;
    // 聚合同一主楼的所有二级评论 =自身id表示一级评论， =所属的一级评论id
    @TableField("root_id")
    private Long rootId;
    @TableField("reply_to_comment_id")
    // 回复的评论id 0表示不是回复评论
    private Long replyToCommentId;
    // 回复的评论用户昵称 null表示不是回复评论
    @TableField("reply_user_name")
    private String replyUserName;
    private Integer status;
    private Integer likeCount;
    private String content;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
