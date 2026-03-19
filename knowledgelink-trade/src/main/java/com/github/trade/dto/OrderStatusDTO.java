package com.github.trade.dto;

import lombok.Data;

/**
 * 订单状态修改dto
 *
 * @author ning
 * @date 2026-03-19
 */
@Data
public class OrderStatusDTO {
    private Long id;
    private String status;
}
