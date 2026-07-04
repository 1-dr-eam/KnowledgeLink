package com.github.trade.dto;

import lombok.Data;

/**
 * 购物车添加和修改数据dto
 *
 * @author ning
 * @date 2026/03/16
 */
@Data
public class CartUpsertRequest {
    private String id;
    private Integer count;
}
