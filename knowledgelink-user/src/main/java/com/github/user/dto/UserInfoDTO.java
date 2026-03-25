package com.github.user.dto;

import lombok.Data;

/**
 * 用户信息dto
 *
 * @author ning
 * @date 2026/03/25
 */
@Data
public class UserInfoDTO {
    private Long id;
    private String phone;
    private String username;
    private String major;
    private String grade;
    private String avatar;
    private String summary;
}
