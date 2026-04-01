package com.github.forum.dto;

import lombok.Data;


/**
 * 搜索dto
 *
 * @author ning
 * @date 2026/04/01
 */
@Data
public class SearchDTO {
    private String keyword;
    // 排序字段 0:时间 1:点赞数 2:收藏数
    private Integer sort;
    private String label;
    private String subject;
    private String subClassify;
}
