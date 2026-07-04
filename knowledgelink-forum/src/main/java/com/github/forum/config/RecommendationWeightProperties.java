package com.github.forum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 帖子推荐权重属性
 *
 * @author ning
 * @date 2026/04/01
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "forum.recommend.weight")
public class RecommendationWeightProperties {
    public static final double DEFAULT_LIKE_WEIGHT = 2.0D;
    public static final double DEFAULT_COLLECT_WEIGHT = 3.0D;
    public static final double DEFAULT_PAGE_VIEW_WEIGHT = 0.10D;
    public static final double DEFAULT_COMMENT_WEIGHT = 1.20D;
    public static final double DEFAULT_HOT_LIKE_WEIGHT = 1.50D;
    public static final double DEFAULT_HOT_COLLECT_WEIGHT = 2.00D;
    public static final double DEFAULT_BEHAVIOR_SCORE_WEIGHT = 0.55D;
    public static final double DEFAULT_HOT_SCORE_WEIGHT = 0.35D;
    public static final double DEFAULT_FRESH_SCORE_WEIGHT = 0.10D;
    public static final double DEFAULT_FRESH_DECAY_HOURS = 72.0D;

    private double like = DEFAULT_LIKE_WEIGHT;
    private double collect = DEFAULT_COLLECT_WEIGHT;
    private double pageView = DEFAULT_PAGE_VIEW_WEIGHT;
    private double comment = DEFAULT_COMMENT_WEIGHT;
    private double hotLike = DEFAULT_HOT_LIKE_WEIGHT;
    private double hotCollect = DEFAULT_HOT_COLLECT_WEIGHT;
    private double behaviorScore = DEFAULT_BEHAVIOR_SCORE_WEIGHT;
    private double hotScore = DEFAULT_HOT_SCORE_WEIGHT;
    private double freshScore = DEFAULT_FRESH_SCORE_WEIGHT;
    private double freshDecayHours = DEFAULT_FRESH_DECAY_HOURS;
}
