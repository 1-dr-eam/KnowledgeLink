package com.github.user.vo;

import lombok.Data;

@Data
public class UserPreferenceBehaviorVO {
    private Long userId;
    private String userCategories;
    private String userKeywords;
}
