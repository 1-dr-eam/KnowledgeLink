package com.github.common.dto;

import lombok.Data;

/**
 * jWT用户dto
 *
 * @author ning
 * @date 2026/03/23
 */
@Data
public class JwtUserClaimsDTO {
    private Long userId;
    private String phone;
    private String username;
    private String major;
    private String grade;
}
