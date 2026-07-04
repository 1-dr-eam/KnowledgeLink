package com.github.trade.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车vo
 *
 * @author ning
 * @date 2026/03/16
 */
@Data
public class CartVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bookId;
    private Integer count;
    private String bookName;
    private String bookAuthor;
    private String bookPublisher;
    private String bookVersion;
    private Double bookPrice;
    private String bookType;
    private String bookItemCategories;
    private String bookItemKeywords;
    private String bookImage;
    private Integer bookStatus;
    private Boolean inventory;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Double amount;
}
