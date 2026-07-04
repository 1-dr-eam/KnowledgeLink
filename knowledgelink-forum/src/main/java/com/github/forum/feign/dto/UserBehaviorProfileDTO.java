package com.github.forum.feign.dto;

import lombok.Data;

@Data
public class UserBehaviorProfileDTO {
    private Long userId;
    private String userCategories;
    private String userKeywords;
}
