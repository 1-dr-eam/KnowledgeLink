package learning_exchange_platform.model;

import lombok.Data;

import java.math.BigInteger;

//社区用户画像
@Data
public class UserProfile {
    private Integer user_id;
    private Integer post_count;//帖子数量
    private Integer total_views;//总浏览量，下类似
    private Integer total_likes;
    private Integer total_collects;
    private Integer total_comments;
    private Double composite_score;//综合以上数据的综合评分

    public UserProfile(Integer user_id, Integer post_count, Integer total_views, Integer total_likes, Integer total_collects, Integer total_comments, Double composite_score) {
        this.user_id = user_id;
        this.post_count = post_count;
        this.total_views = total_views;
        this.total_likes = total_likes;
        this.total_collects = total_collects;
        this.total_comments = total_comments;
        this.composite_score = composite_score;
    }
}
