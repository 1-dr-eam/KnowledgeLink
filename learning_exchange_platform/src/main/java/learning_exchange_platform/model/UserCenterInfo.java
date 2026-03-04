package learning_exchange_platform.model;

import lombok.Data;

@Data
public class UserCenterInfo {
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

    public UserCenterInfo(String username, String avatar, String summary, String grade, String major, Integer focusCount, Integer fansCount, Integer postCount, Integer likeCount, Integer collectCount, Integer viewCount) {
        this.username = username;
        this.avatar = avatar;
        this.summary = summary;
        this.grade = grade;
        this.major = major;
        this.focusCount = focusCount;
        this.fansCount = fansCount;
        this.postCount = postCount;
        this.likeCount = likeCount;
        this.collectCount = collectCount;
        this.viewCount = viewCount;
    }
}
