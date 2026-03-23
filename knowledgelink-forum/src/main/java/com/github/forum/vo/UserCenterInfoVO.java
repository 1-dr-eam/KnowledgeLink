package com.github.forum.vo;

import lombok.Data;

@Data
public class UserCenterInfoVO {
    private String username;
    private String avatar;
    private String summary;
    private String grade;
    private String major;
    private Integer focusCount;
    private Integer fansCount;
    private Integer postCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer viewCount;
}
