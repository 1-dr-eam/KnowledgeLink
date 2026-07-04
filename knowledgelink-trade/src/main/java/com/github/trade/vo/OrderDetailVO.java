package com.github.trade.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单信息返回VO
 *
 * @author ning
 * @date 2026-03-19
 */
@Data
public class OrderDetailVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sellerId;
    private String sellerName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long bookId;
    private String image;
    private double price;
    private Integer count;
    private double totalPrice;
    private String bookName;
    private String bookAuthor;
    private String bookPublisher;
    private String bookDescription;
    private String bookVersion;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime finishedTime;
    private String address;
}
