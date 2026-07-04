package com.github.trade.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量ID请求dto
 *
 * @author ning
 * @date 2026-03-18
 */
@Data
public class BatchIdRequest {
    private List<String> ids;
    private String address;
}
