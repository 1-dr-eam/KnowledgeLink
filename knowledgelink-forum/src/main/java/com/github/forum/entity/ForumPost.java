package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_post")
public class ForumPost {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String summary;
    private String content;
    private String authorName;
    private String coverAvatar;
    private String label;
    private String type;
    private String visibleRange;
    private Integer pageViews;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private String subject;
    private String subClassify;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
