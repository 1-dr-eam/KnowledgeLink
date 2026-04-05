package com.github.trade.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ning
 * @date 2026/03/16
 */
@Data
public class BookEsDocument {
    private Long itemId;
    private Long sellerId;
    private String city;
    private String name;
    private String author;
    private String publisher;
    private String version;
    private double price;
    private String type;
    private String itemCategories;
    private String itemKeywords;
    private Boolean note;
    private String description;
    private Integer count;
    private Integer status;
    private String image;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
