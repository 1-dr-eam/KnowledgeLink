package com.github.trade.dto;

import lombok.Data;

/**
 * 购物车添加数据dto
 *
 * @author ning
 * @date 2026/03/16
 */
@Data
public class ShoppingCarAddDTO {
    private Long bookId;
    private Integer count;
}
