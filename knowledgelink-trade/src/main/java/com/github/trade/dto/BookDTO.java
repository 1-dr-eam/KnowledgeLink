package com.github.trade.dto;

import lombok.Data;

import java.util.List;

/**
 * @author ning
 * @date 2026-03-11
 */
@Data
public class BookDTO {
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
    private List<String> image;
}
