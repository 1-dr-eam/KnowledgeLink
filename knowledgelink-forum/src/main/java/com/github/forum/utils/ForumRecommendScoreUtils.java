package com.github.forum.utils;

import com.github.forum.config.RecommendationWeightProperties;
import com.github.forum.entity.Forum;
import com.github.forum.entity.UserForumInteractions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 论坛评分推荐工具类
 *
 * @author ning
 * @date 2026/04/01
 */
@Component
public class ForumRecommendScoreUtils {
    private final double likeWeight;
    private final double collectWeight;
    private final double pageViewWeight;
    private final double commentWeight;
    private final double hotLikeWeight;
    private final double hotCollectWeight;
    private final double behaviorScoreWeight;
    private final double hotScoreWeight;
    private final double freshScoreWeight;
    private final double freshDecayHours;

    public ForumRecommendScoreUtils(RecommendationWeightProperties properties) {
        this.likeWeight = properties.getLike();
        this.collectWeight = properties.getCollect();
        this.pageViewWeight = properties.getPageView();
        this.commentWeight = properties.getComment();
        this.hotLikeWeight = properties.getHotLike();
        this.hotCollectWeight = properties.getHotCollect();
        this.behaviorScoreWeight = properties.getBehaviorScore();
        this.hotScoreWeight = properties.getHotScore();
        this.freshScoreWeight = properties.getFreshScore();
        this.freshDecayHours = properties.getFreshDecayHours();
    }

    public double calculateBehaviorScore(UserForumInteractions interaction) {
        if (interaction == null) {
            return 0D;
        }
        double score = 0D;
        if (Boolean.TRUE.equals(interaction.getLikeStatus())) {
            score += likeWeight;
        }
        if (Boolean.TRUE.equals(interaction.getCollectStatus())) {
            score += collectWeight;
        }
        return score;
    }

    public double calculateHotScore(Forum forum) {
        if (forum == null) {
            return 0D;
        }
        double pageViews = safeNonNegative(forum.getPageViews());
        double comments = safeNonNegative(forum.getCommentCount());
        double likes = safeNonNegative(forum.getLikeCount());
        double collects = safeNonNegative(forum.getCollectCount());
        return Math.log1p(pageViews) * pageViewWeight
                + Math.log1p(comments) * commentWeight
                + Math.log1p(likes) * hotLikeWeight
                + Math.log1p(collects) * hotCollectWeight;
    }

    public double calculateFreshScore(Forum forum) {
        if (forum == null || forum.getCreateTime() == null) {
            return 0D;
        }
        long ageSeconds = Math.max(Duration.between(forum.getCreateTime(), LocalDateTime.now()).getSeconds(), 0L);
        double ageHours = ageSeconds / 3600D;
        return Math.exp(-ageHours / Math.max(freshDecayHours, 1D));
    }

    public double calculateTotalScore(Forum forum, UserForumInteractions interaction) {
        double behaviorScore = calculateBehaviorScore(interaction);
        double hotScore = calculateHotScore(forum);
        double freshScore = calculateFreshScore(forum);
        return behaviorScoreWeight * behaviorScore
                + hotScoreWeight * hotScore
                + freshScoreWeight * freshScore;
    }

    private double safeNonNegative(Integer value) {
        if (value == null || value < 0) {
            return 0D;
        }
        return value.doubleValue();
    }
}
