package com.github.trade.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_address")
public class UserAddress {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("user_id")
    private Long userId;
    @TableField("receiver_name")
    private String receiverName;
    @TableField("receiver_phone")
    private String receiverPhone;
    @TableField("province")
    private String province;
    @TableField("city")
    private String city;
    @TableField("district")
    private String district;
    @TableField("detail_address")
    private String detailAddress;
    @TableField("is_default")
    private Boolean isDefault;
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
