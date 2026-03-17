package com.github.common.dto;

import lombok.Data;

/**
 * @author ningning
 * @date 2026-03-10
 * 用户信息传输类
 */
@Data
public class UserDTO {
    private Long id;
    private String phone;
    private String username;
    private String major;
    private String grade;
    private String avatar;
    private String summary;
    private double balance;
}
