package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_reply_comment")
public class ForumReplyComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long commentId;
    private String replyType;
    private Long replyCommentId;
    private String replyUserName;
    private Long userId;
    private String userName;
    private String content;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
