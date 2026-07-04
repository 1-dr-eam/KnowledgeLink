package com.github.user.vo;

import lombok.Data;

@Data
public class UserDashboardVO {
    private String username;
    private String avatar;
    private String grade;
    private String major;
    private String shippingAddress;
    private Integer postPageViews;
    private Integer postLikeCount;
    private Integer postCollectCount;
    private Integer followersCount;
    private Integer followCount;
    private Integer friendCount;
}
