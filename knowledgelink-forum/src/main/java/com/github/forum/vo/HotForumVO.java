package com.github.forum.vo;

import lombok.Data;

/**
 * 热门帖子vo
 *
 * @author ning
 * @date 2026/03/29
 */
@Data
public class HotForumVO {
    private String id;
    private String title;
    private Long hot;
}
