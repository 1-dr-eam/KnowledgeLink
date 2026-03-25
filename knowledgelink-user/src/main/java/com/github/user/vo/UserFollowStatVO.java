package com.github.user.vo;

import lombok.Data;

/**
 * 用户关注统计返回对象
 *
 * @author ning
 * @date 2026/03/25
 */
@Data
public class UserFollowStatVO {
    private Integer followCount;
    private Integer followersCount;
}
