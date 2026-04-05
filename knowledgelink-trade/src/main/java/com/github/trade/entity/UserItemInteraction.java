package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户商品交互记录
 *
 * @author ning
 * @date 2026/04/05
 */
@Data
@TableName("user_item_interaction")
public class UserItemInteraction {
    @TableId("id")
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("item_id")
    private Long itemId;
    // 等于click + cart + forward + buy
    private Integer rating;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime dateTime;
    private Integer hour;
    @TableField("is_weekend")
    private Boolean weekend;
    @TableField("is_holiday")
    private Boolean holiday;
    private Boolean click;
    private Boolean cart;
    private Boolean forward;
    private Boolean buy;
}
