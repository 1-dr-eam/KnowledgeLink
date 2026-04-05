package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品交互记录
 *
 * @author ning
 * @date 2026/04/05
 */
@Data
@TableName("item_interaction")
public class ItemInteraction {
    @TableId("item_id")
    private Long itemId;
    @TableField("item_click_last_3m")
    private Integer itemClickLast3M;
    @TableField("item_cart_last_3m")
    private Integer itemCartLast3M;
    @TableField("item_buy_last_3m")
    private Integer itemBuyLast3M;
    @TableField("item_forward_last_3m")
    private Integer itemForwardLast3M;
    @TableField( value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField( value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
