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
public class ShoppingCarVO {
    private Integer id;
    private Integer userId;
    private Integer bookId;
    private Integer count;
    private String bookName;
    private String bookAuthor;
    private String bookPublisher;
    private String bookVersion;
    private Double bookPrice;
    private String bookType;
    private String bookClassify;
    private String bookSubClassify;
    private List<String> bookAvatar;
    private Integer bookStatus;
    private Boolean inventory;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Double amount;
}
