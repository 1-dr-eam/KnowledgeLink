package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 订单实体类
 *
 * @author ning
 * @date 2026/03/19
 */
@Data
@TableName("order")
public class Order {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("seller_id")
    private Long sellerId;
    @TableField("book_id")
    private Long bookId;
    private double price;
    private Integer count;
    @TableField("total_price")
    private double totalPrice;
    @TableField("book_name")
    private String bookName;
    private String status;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill =  FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @TableField("finished_time")
    private LocalDateTime finishedTime;
    private String address;
}