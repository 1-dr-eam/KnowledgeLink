package com.github.trade.vo;

import lombok.Data;

/**
 * @author ning
 * @date 2026-03-19
 */
@Data
public class OrderSynoVO {
    private Long id;
    private String image;
    private String bookName;
    private String bookAuthor;
    private String bookPublisher;
    private String bookVersion;
    private Integer count;
    private Double price;
    private Double totalPrice;
    private String status;
}
