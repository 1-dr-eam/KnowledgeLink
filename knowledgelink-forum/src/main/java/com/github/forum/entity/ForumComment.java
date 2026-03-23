package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_comment")
public class ForumComment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long postId;
    private Long userId;
    private String userName;
    private String content;
    private Integer replyCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
