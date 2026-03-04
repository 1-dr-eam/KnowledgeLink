package learning_exchange_platform.service;

import learning_exchange_platform.mapper.UserPostBehaviorMapper;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.UserPostBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HybridPostRecommendService {

    @Autowired
    private ContentBasedPostRecommendService contentBasedService;//基于内容

    @Autowired
    private CollaborativeFilteringPostRecommendService cfService;//基于协同过滤

    @Autowired
    private UserPostBehaviorMapper userPostBehaviorMapper;

    /**
     * 混合推荐
     * 用基于内容的方法解决冷启动，用协同过滤增加多样性和新颖性
     */
    public List<Post> getHybridRecommendations(int user_id, int limit) {
        // 1. 获取用户历史行为，判断是否为新用户
        List<UserPostBehavior> userBehaviors = userPostBehaviorMapper.selectPostBehaviorByUserId(user_id);
        boolean isEmptyUser= userBehaviors.isEmpty();
        boolean isNewUser = userBehaviors.size() < 5;

        // 2. 根据用户类型调整权重
        double contentWeight, cfWeight;

        if(isEmptyUser) {
            return contentBasedService.getContentBasedRecommendations(user_id, limit * 2);
        }else {
            if (isNewUser) {
                // 新用户：主要依赖基于内容的推荐
                contentWeight = 0.8;
                cfWeight = 0.2;
            } else {
                // 老用户：平衡两种方法，根据行为丰富度动态调整
                int behaviorCount = userBehaviors.size();
                double cfRatio = Math.min(0.7, behaviorCount / 100.0); // 最大70%的CF权重
                contentWeight = 1.0 - cfRatio;
                cfWeight = cfRatio;
            }

            // 3. 并行获取两种推荐结果
            List<Post> contentRecommendations = contentBasedService.getContentBasedRecommendations(user_id, limit * 2);
            List<Post> cfRecommendations = cfService.getItemBasedCFRecommendations(user_id, limit * 2);//默认用基于物品的

            // 4. 如果基于物品的协同过滤结果不足，使用基于用户的协同过滤作为补充
            if (cfRecommendations.size() < limit / 2) {
                List<Post> userCFRecommendations = cfService.getUserBasedCFRecommendations(user_id, limit);
                cfRecommendations.addAll(userCFRecommendations);
            }

            // 5. 混合推荐结果
            return hybridMergeRecommendations(contentRecommendations, cfRecommendations,
                    contentWeight, cfWeight, limit);
        }

    }

    /**
     * 混合合并推荐结果
     */
    private List<Post> hybridMergeRecommendations(List<Post> contentRecs,
                                                  List<Post> cfRecs,
                                                  double contentWeight,
                                                  double cfWeight,
                                                  int limit) {
        Map<Integer, HybridPostScore> postScores = new HashMap<>();//帖子ID-混合推荐得分

        // 计算基于内容推荐的得分
        for (int i = 0; i < contentRecs.size(); i++) {
            Post post = contentRecs.get(i);
            double score = contentWeight * (1.0 - (double) i / contentRecs.size());//越靠前的说明越相似，得分越高
            postScores.putIfAbsent(post.getId(), new HybridPostScore(post));
            postScores.get(post.getId()).addContentScore(score);
        }

        // 计算协同过滤推荐的得分
        for (int i = 0; i < cfRecs.size(); i++) {
            Post post = cfRecs.get(i);
            double score = cfWeight * (1.0 - (double) i / cfRecs.size());
            postScores.putIfAbsent(post.getId(), new HybridPostScore(post));
            postScores.get(post.getId()).addCfScore(score);
        }

        // 按综合得分排序
        return postScores.values().stream()
                .sorted(Comparator.comparing(HybridPostScore::getTotalScore).reversed())
                .map(HybridPostScore::getPost)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取个性化热点推荐（结合热点和用户兴趣）
     */
    public List<Post> getPersonalizedHotRecommendations(int user_id, int limit) {
        List<Post> hotspotPosts = contentBasedService.getHotspotRecommendations(limit);
        List<Post> personalizedPosts = getHybridRecommendations(user_id, limit);

        // 混合热点和个性化推荐（各50%）
        Set<Post> mixedPosts = new HashSet<>();
        int hotspotCount = limit / 2;
        int personalCount = limit - hotspotCount;

        mixedPosts.addAll(hotspotPosts.subList(0, Math.min(hotspotCount, hotspotPosts.size())));
        mixedPosts.addAll(personalizedPosts.subList(0, Math.min(personalCount, personalizedPosts.size())));

        return new ArrayList<>(mixedPosts);
    }

    /**
     * 内部类用于存储混合推荐得分
     */
    private static class HybridPostScore {
        private Post post;
        private double contentScore = 0;
        private double cfScore = 0;

        public HybridPostScore(Post post) {
            this.post = post;
        }

        public void addContentScore(double score) {
            this.contentScore += score;
        }

        public void addCfScore(double score) {
            this.cfScore += score;
        }

        public double getTotalScore() {
            return contentScore + cfScore;
        }

        public Post getPost() {
            return post;
        }
    }
}