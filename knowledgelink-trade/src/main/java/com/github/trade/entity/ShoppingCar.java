package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 购物车
 *
 * @author ning
 * @date 2026/03/12
 */
@Data
@TableName("shopping_car")
public class ShoppingCar {
        @TableId(type = IdType.ASSIGN_ID)
        private Long id;
        @TableField("user_id")
        private Long userId;
        @TableField("book_id")
        private Long bookId;
        private Integer count;
        @TableField(value = "create_time", fill = FieldFill.INSERT)
        private LocalDateTime createTime;
        @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
        private LocalDateTime updateTime;
        private Boolean inventory;
}
