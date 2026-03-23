package com.github.forum.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("forum_shopping_address")
public class ForumShoppingAddress {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String shoppingAddress;
    private String label;
    private Boolean defaultFlag;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
