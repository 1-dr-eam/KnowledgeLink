package com.github.trade.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 推荐接口健康检查
 *
 * @author ning
 * @date 2026/04/05
 */
@Data
public class RecommendHealthResponse {
    private String health;
    private String reason;
    private LocalDateTime timestamp;
}
