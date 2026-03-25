package com.github.user.vo;

import lombok.Data;

/**
 * 用户信息返回对象
 *
 * @author ning
 * @date 2026/03/25
 */
@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String major;
    private String grade;
    private String avatar;
    private String summary;
}
