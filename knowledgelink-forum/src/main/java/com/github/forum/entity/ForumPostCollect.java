package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_post_collect")
public class ForumPostCollect {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long postId;
    private LocalDateTime createTime;
}
