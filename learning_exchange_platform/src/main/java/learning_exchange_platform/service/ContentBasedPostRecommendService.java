package learning_exchange_platform.service;

import learning_exchange_platform.mapper.PostMapper;
import learning_exchange_platform.mapper.UserPostBehaviorMapper;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.UserPostBehavior;
import learning_exchange_platform.utils.PostScoreComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service
@Transactional
public class ContentBasedPostRecommendService {

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private UserPostBehaviorMapper UserPostBehaviorMapper;

    /**
     * 基于内容的帖子推荐
     * 根据用户历史行为中帖子的内容特征推荐相似帖子
     */
    public List<Post> getContentBasedRecommendations(int user_id, int limit) {
        // 1. 获取用户的历史行为帖子（浏览、点赞、收藏）
        List<Post> userViewedPosts = UserPostBehaviorMapper.selectUserViewedPosts(user_id);
        List<Post> userLikedPosts = UserPostBehaviorMapper.selectUserLikedPosts(user_id);
        List<Post> userCollectedPosts = UserPostBehaviorMapper.selectUserCollectedPosts(user_id);

        // 合并所有用户感兴趣的帖子，并按权重排序（收藏 > 点赞 > 浏览）
        List<Post> weightedInterestPosts=getPostsWithWeight(userCollectedPosts, userLikedPosts, userViewedPosts);

        //为空说明是新用户，用默认推荐
        if (weightedInterestPosts.isEmpty()) {
            return getDefaultContentRecommendations(limit);
        }

        // 2. 基于用户感兴趣的帖子特征进行推荐
        Set<Post> recommendations = new HashSet<>();

        for (Post interestPost : weightedInterestPosts) {
            // 对于每个用户感兴趣的帖子，找到内容相似的帖子
            List<Post> similarPosts = postMapper.selectContentSimilarPosts(
                    interestPost.getSubject(),
                    interestPost.getLabel(),
                    interestPost.getSub_classify(),
                    interestPost.getId(),
                    limit / 3 + 1
            );
            recommendations.addAll(similarPosts);

            // 如果已经收集足够多的推荐，提前退出
            if (recommendations.size() >= limit * 2) {
                break;
            }
        }

        // 3. 如果推荐数量不足，补充默认推荐
        if (recommendations.size() < limit) {
            List<Post> defaultRecs = getDefaultContentRecommendations(limit - recommendations.size());
            recommendations.addAll(defaultRecs);
        }

        // 4. 过滤掉用户已经接触过的帖子
        List<Integer> userInteractedPostIds = getUserInteractedPostIds(user_id);
        List<Post> filteredRecommendations = recommendations.stream()
                .filter(post -> !userInteractedPostIds.contains(post.getId()))
                .collect(Collectors.toList());

        return filteredRecommendations.subList(0, Math.min(limit, filteredRecommendations.size()));
    }

    /**
     * 综合考虑帖子本身的质量和用户行为的影响，得到综合排序列表
     */
    private List<Post> getPostsWithWeight(List<Post> userCollectedPosts,List<Post> userLikedPosts,List<Post> userViewedPosts) {
        //收藏，点赞，浏览的权重
        List<Double> weights = List.of(0.5,0.3,0.2);
        Map<Post,Double> post_score=new HashMap<>();
        PostScoreComparator postScoreComparator = new PostScoreComparator();
        for(Post post : userCollectedPosts) {
            post_score.put(post,postScoreComparator.getScore(post)*weights.get(0));
        }
        for(Post post : userLikedPosts) {
            post_score.merge(post,postScoreComparator.getScore(post)*weights.get(1),Double::sum);
        }
        for(Post post : userViewedPosts) {
            post_score.merge(post,postScoreComparator.getScore(post)*weights.get(2),Double::sum);
        }
        List<Post> recommend_posts = post_score.entrySet()
                .stream()
                .sorted(Map.Entry.<Post, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return recommend_posts;
    }

    /**
     * 获取用户已经交互过的帖子ID
     */
    private List<Integer> getUserInteractedPostIds(int user_id) {
        List<UserPostBehavior> userBehaviors = UserPostBehaviorMapper.selectPostBehaviorByUserId(user_id);
        if(userBehaviors.isEmpty()) {
            return null;
        }
        List<Integer> ids = new ArrayList<>();
        for(UserPostBehavior userBehavior : userBehaviors) {
            ids.add(userBehavior.getPost_id());
        }
        return ids;
    }

    /**
     * 默认推荐（用于新用户）
     */
    private List<Post> getDefaultContentRecommendations(Integer limit) {
        // 返回热门帖子（综合热度和时间排序）
        List<Post> posts = postMapper.selectAllPosts();
        posts.sort(new PostScoreComparator());
        return posts.subList(0, Math.min(limit, posts.size()));//限定返回个数
    }

    /**
     * 提取帖子特征向量（用于更精确的内容匹配）
     */
    public Map<String, Double> extractPostFeatures(Post post) {
        Map<String, Double> features = new HashMap<>();

        // 基于主题、标签、子分类等构建特征向量
        if (post.getSubject() != null) {
            features.put("subject_" + post.getSubject(), 1.0);
        }
        if (post.getSub_classify() != null) {
            features.put("subclassify_" + post.getSub_classify(), 0.8);
        }
        if (post.getLabel() != null) {
            features.put("label_" + post.getLabel(), 0.6);
        }
        if (post.getType() != null) {
            features.put("type_" + post.getType(), 0.4);
        }

        // 可以添加基于标题关键词的特征
        if (post.getTitle() != null) {
            // 简单的关键词提取（实际应用中可以使用TF-IDF等算法）
            String[] keywords = extractKeywords(post.getTitle());
            for (String keyword : keywords) {
                features.put("keyword_" + keyword, 0.3);
            }
        }

        return features;
    }

    /**
     * 简单的关键词提取
     */
    private String[] extractKeywords(String text) {
        // 移除标点符号，分割成单词
        String cleanedText = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", " ");
        return cleanedText.split("\\s+");
    }

    /**
     * 热度和新鲜帖子各取一部分推荐
     */
    public List<Post> getHotspotRecommendations(Integer limit) {
        // 结合热门度和新鲜度
        List<Post> popularPosts = postMapper.selectPopularPosts(limit * 2);
        List<Post> latestPosts = postMapper.selectLatestPosts(limit);

        // 混合推荐：70%热门帖子 + 30%最新帖子
        Set<Post> mixedPosts = new HashSet<>();
        int popularCount = (int) (limit * 0.7);
        int latestCount = limit - popularCount;

        mixedPosts.addAll(popularPosts.subList(0, Math.min(popularCount, popularPosts.size())));
        mixedPosts.addAll(latestPosts.subList(0, Math.min(latestCount, latestPosts.size())));

        return new ArrayList<>(mixedPosts);
    }
}