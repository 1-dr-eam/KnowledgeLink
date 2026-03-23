package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_comment_like")
public class ForumCommentLike {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long commentId;
    private LocalDateTime createTime;
}
