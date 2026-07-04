package com.github.trade.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author ning
 * @date 2026-04-13
 */
@Data
public class FineTuningRequest {
    @JsonProperty("start_time")
    private String startTime;
    @JsonProperty("end_time")
    private String endTime;
}
