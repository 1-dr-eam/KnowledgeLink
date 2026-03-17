package com.github.trade.entity;

import java.time.LocalDateTime;

public class Order {
    private Integer id;
    private Integer userId;
    private Integer sellerId;
    private Integer bookId;
    private Integer count;
    private double totalPrice;
    private String bookName;
    private String status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime finishedTime;
    private String address;
}