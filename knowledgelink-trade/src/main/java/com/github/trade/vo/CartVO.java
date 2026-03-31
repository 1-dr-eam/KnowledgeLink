package com.github.trade.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 购物车vo
 *
 * @author ning
 * @date 2026/03/16
 */
@Data
public class CartVO {
    private Long id;
    private Long userId;
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
    private List<String> bookImage;
    private Integer bookStatus;
    private Boolean inventory;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Double amount;
}
