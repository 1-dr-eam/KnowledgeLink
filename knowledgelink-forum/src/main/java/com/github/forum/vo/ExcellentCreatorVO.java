package com.github.forum.vo;

import lombok.Data;

/**
 * 优秀创作者vo
 *
 * @author ning
 * @date 2026/03/29
 */
@Data
public class ExcellentCreatorVO {
    private String id;
    private String avatar;
    private String name;
    private Long score;
    private String intro;
}
