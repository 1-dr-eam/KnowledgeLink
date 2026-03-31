package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author ning
 * @date 2026/03/10
 */
@Data
@TableName("book_info")
public class Book {
    @TableId(value = "item_id", type = IdType.ASSIGN_ID)
    private Long itemId;
    @TableField("seller_id")
    private Long sellerId;
    private String city;
    private String name;
    private String author;
    private String publisher;
    private String version;
    private double price;
    private String type;
    @TableField("item_categories")
    private String itemCategories;
    @TableField("item_keywords")
    private String itemKeywords;
    private Boolean note;
    private String description;
    private Integer count;
    private Integer status;
    private String image;
    @TableField(value = "created_time", fill = FieldFill.INSERT)
    private LocalDateTime createdTime;
    @TableField(value = "updated_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
