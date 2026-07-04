package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 热门帖子
 * 用于记录帖子的id, title, score
 * 前十名作为排行榜中的帖子
 * 所有的帖子在用户个性化推荐时随机抽取20条加入推荐列表
 *
 * @author ning
 * @date 2026/04/01
 */
@Data
@TableName("hot_forum")
public class HotForum {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long forumId;
    private String title;
    private Integer score;
}
