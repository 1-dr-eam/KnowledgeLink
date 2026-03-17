package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
        @TableId(type = IdType.AUTO)
        private Integer id;
        @TableField("user_id")
        private Integer userId;
        @TableField("book_id")
        private Integer bookId;
        private Integer count;
        @TableField("create_time")
        private LocalDateTime createTime;
        @TableField("update_time")
        private LocalDateTime updateTime;
        private Boolean inventory;
}
