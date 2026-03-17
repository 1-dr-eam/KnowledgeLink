package com.github.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ning
 * @date 2026/03/16
 */
@Data
public class BookEsDocument {
    private Long id;
    private Long sellerId;
    private String name;
    private String author;
    private String publisher;
    private String version;
    private double price;
    private String type;
    private String classify;
    private String subClassify;
    private Boolean note;
    private String description;
    private Integer status;
    private String avatar;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
