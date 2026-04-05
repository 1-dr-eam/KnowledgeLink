package com.github.trade.dto;

import lombok.Data;

import java.util.List;

/**
 * 书籍推荐IDS响应
 *
 * @author ning
 * @date 2026/04/05
 */
@Data
public class RecommendIdsResponse {
    private List<Object> recommendIds;
}
