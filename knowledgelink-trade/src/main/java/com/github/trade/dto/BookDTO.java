package com.github.trade.dto;

import lombok.Data;

import java.util.List;

/**
 * @author ning
 * @date 2026-03-11
 */
@Data
public class BookDTO {
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
    private List<String> avatar;
}
