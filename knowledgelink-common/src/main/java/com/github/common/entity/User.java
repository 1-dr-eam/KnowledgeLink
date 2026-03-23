package com.github.common.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @author ning
 * @date 2026/03/10
 * 用户类
 */
@Data
@Builder
public class User {
    private Long id;
    private final String phone;
    private final String username;
    private final String password;
    private final String major;
    private final String grade;
    private String avatar;
    private String summary;
}
