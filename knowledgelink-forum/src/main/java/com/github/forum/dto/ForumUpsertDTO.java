package com.github.forum.dto;

import lombok.Data;

/**
 * 帖子dto
 * 用于传递创建或者更新帖子的参数
 *
 * @author ning
 * @date 2026-03-31
 */
@Data
public class ForumUpsertDTO {
    private String title;
    private String summary;
    private String content;
    private String coverAvatar;
    private String label;
    private String subject;
    private String subClassify;
    private String type;
    private String visibleRange;
}
