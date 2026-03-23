package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("forum_news")
public class ForumNews {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private LocalDate publishDate;
    private Integer clickCount;
    private String summary;
    private String content;
    private String avatar;
}
