package com.github.trade.dto;

import lombok.Data;

/**
 * 商品下单dto
 *
 * @author ning
 * @date 2026-03-19
 */
@Data
public class OrderAddDTO {
    private Long bookId;
    private Integer count;
    private String address;
}
