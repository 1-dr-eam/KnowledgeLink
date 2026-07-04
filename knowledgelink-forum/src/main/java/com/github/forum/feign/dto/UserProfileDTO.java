package com.github.forum.feign.dto;

import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private String avatar;
}
