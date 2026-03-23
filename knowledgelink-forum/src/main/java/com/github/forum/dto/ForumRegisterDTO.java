package com.github.forum.dto;

import lombok.Data;

@Data
public class ForumRegisterDTO {
    private String phone;
    private String username;
    private String password;
    private String confirmPassword;
    private String grade;
    private String major;
}
