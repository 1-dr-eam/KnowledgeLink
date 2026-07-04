package com.github.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户行为画像实体类
 *
 * @author ning
 * @date 2026/04/06
 */
@Data
@TableName("user_profile_behavior")
public class UserProfileBehavior {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("user_categories")
    private String userCategories;
    @TableField("user_keywords")
    private String userKeywords;
    private Integer age;
    private String gender;
    @TableField("user_click_last3m")
    private Integer userClickLast3M;
    @TableField("user_cart_last3m")
    private Integer userCartLast3M;
    @TableField("user_buy_last3m")
    private Integer userBuyLast3M;
    @TableField("user_forward_last3m")
    private Integer userForwardLast3M;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
