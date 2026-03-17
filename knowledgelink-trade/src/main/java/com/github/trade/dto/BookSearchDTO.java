package com.github.trade.dto;

import lombok.Data;

/**
 * @author ning
 * @date 2026/03/10
 */
@Data
public class BookSearchDTO {
    private Long id;
    private String searchKeyword;
    // note : true->有笔记 false->无笔记
    private Boolean note;
    private String type;
    // sort : 1->升序 2->降序
    private Integer sort;
}
