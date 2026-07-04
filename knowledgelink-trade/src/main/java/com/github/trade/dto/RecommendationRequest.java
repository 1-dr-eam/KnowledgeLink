package com.github.trade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 推荐请求
 *
 * @author ning
 * @date 2026/04/09
 */
@Data
public class RecommendationRequest {
    @JsonProperty("user_id")
    private Long userId;

    private int hour;

    @JsonProperty("is_weekend")
    private boolean isWeekend;

    @JsonProperty("is_holiday")
    private boolean isHoliday;
}
