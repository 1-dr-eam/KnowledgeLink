package com.github.trade.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * @author ning
 * @date 2026-03-11
 */
@Data
public class BookDTO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long itemId;
    @JsonSerialize(using = ToStringSerializer.class)
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
}
