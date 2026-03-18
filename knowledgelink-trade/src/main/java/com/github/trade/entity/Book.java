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
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("seller_id")
    private Long sellerId;
    private String name;
    private String author;
    private String publisher;
    private String version;
    private double price;
    private String type;
    private String classify;
    @TableField("sub_classify")
    private String subClassify;
    private Boolean note;
    private String description;
    private Integer count;
    private Integer status;
    private String avatar;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
