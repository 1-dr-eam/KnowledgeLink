package learning_exchange_platform.utils;

import learning_exchange_platform.model.Post;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;

public class PostScoreComparator implements Comparator<Post> {
    // 权重配置
    private final double viewWeight;
    private final double collectWeight;
    private final double likeWeight;
    private final double commentWeight;
    private final boolean considerTime; // 是否考虑时间因素

    // 构造方法，可以灵活配置权重
    public PostScoreComparator(double viewWeight, double collectWeight, double likeWeight, double commentWeight,boolean considerTime) {
        this.viewWeight = viewWeight;
        this.collectWeight = collectWeight;
        this.likeWeight = likeWeight;
        this.commentWeight = commentWeight;
        this.considerTime = considerTime;
    }

    // 默认构造方法使用常用权重
    public PostScoreComparator() {
        this(0.3, 0.3, 0.2,0.2, true);
    }

    @Override
    public int compare(Post p1, Post p2) {
        double score1 = calculateScore(p1);
        double score2 = calculateScore(p2);

        return Double.compare(score2, score1); // 降序排列
    }

    public double getScore(Post p) {
        return calculateScore(p);
    }

    /**
     * 计算帖子的综合评分
     */
    private double calculateScore(Post post) {
        double baseScore = post.getPage_views() * viewWeight/100.0
                + post.getCollect_count() * collectWeight
                + post.getLike_count() * likeWeight
                + post.getComment_count() * commentWeight;

        if (considerTime) {
            // 加入时间衰减因子（新帖子有加分）
            return baseScore * getTimeFactor(post.getPublish_date());
        }

        return baseScore;
    }

    /**
     * 时间衰减因子：新发布的帖子有额外加分
     */
    private double getTimeFactor(LocalDate publishDate) {
        LocalDate now = LocalDate.now();
        // 计算天数差
        long daysBetween = ChronoUnit.DAYS.between(now, publishDate);
        // 时间越短的加分越多
        if (daysBetween <= 7) return 2;
        if (daysBetween <= 30) return 1.6;
        if (daysBetween<=365) return 1.2;
        return 1.0;
    }
}
