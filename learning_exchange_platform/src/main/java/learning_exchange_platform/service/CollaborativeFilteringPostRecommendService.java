package learning_exchange_platform.service;

import learning_exchange_platform.mapper.PostMapper;
import learning_exchange_platform.mapper.UserPostBehaviorMapper;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.UserPostBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

//基于协同过滤的推荐，无法解决新用户冷启动问题，这个问题用基于内容的推荐解决
@Service
@Transactional
public class CollaborativeFilteringPostRecommendService {

    @Autowired
    private UserPostBehaviorMapper userPostBehaviorMapper;

    @Autowired
    private PostMapper postMapper;

    /**
     * 基于用户的协同过滤推荐
     */
    public List<Post> getUserBasedCFRecommendations(Integer user_id, Integer limit) {
        // 1. 获取所有用户行为数据
        List<UserPostBehavior> allBehaviors = userPostBehaviorMapper.selectAllUserBehaviors();

        // 2. 构建用户-帖子矩阵（带时间衰减）
        Map<Integer, Map<Integer, Double>> userPostMatrix = buildUserPostMatrixWithTimeDecay(allBehaviors);

        // 3. 计算用户相似度
        Map<Integer, Double> userSimilarities = calculateUserSimilarities(user_id, userPostMatrix);

        // 4. 生成推荐
        return generateUserBasedRecommendations(user_id, userSimilarities, userPostMatrix, limit);
    }

    /**
     * 基于物品的协同过滤推荐
     */
    public List<Post> getItemBasedCFRecommendations(Integer user_id, Integer limit) {
        // 1. 获取用户的历史行为
        List<UserPostBehavior> userBehaviors = userPostBehaviorMapper.selectPostBehaviorByUserId(user_id);

        if (userBehaviors.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 获取所有行为数据构建帖子相似度矩阵
        List<UserPostBehavior> allBehaviors = userPostBehaviorMapper.selectAllUserBehaviors();
        Map<Integer, Map<Integer, Double>> postSimilarityMatrix = buildPostSimilarityMatrix(allBehaviors);

        // 3. 基于用户喜欢的帖子找到相似帖子
        return generateItemBasedRecommendations(userBehaviors, postSimilarityMatrix, limit);
    }

    /**
     * 构建带时间衰减的用户-帖子矩阵（用户对应帖子-互动得分映射）
     */
    private Map<Integer, Map<Integer, Double>> buildUserPostMatrixWithTimeDecay(List<UserPostBehavior> behaviors) {
        Map<Integer, Map<Integer, Double>> matrix = new HashMap<>();
        Long currentTime = System.currentTimeMillis() / 1000; // 当前时间戳（秒）

        for (UserPostBehavior behavior : behaviors) {
            matrix.putIfAbsent(behavior.getUser_id(), new HashMap<>());//添加某用户，对应一个<帖子ID，分数>的映射

            int navigate_time=behavior.getNavigate_time();
            double baseScore = getBehaviorScore(behavior.getBehavior_type(),navigate_time);//根据行为类型得到基础分数
            double timeDecay = calculateTimeDecay(behavior, currentTime);//获取时间衰减因子，行为越久远衰减的越多
            double finalScore = baseScore * timeDecay;

            matrix.get(behavior.getUser_id()).put(behavior.getPost_id(), finalScore);
        }
        return matrix;
    }

    /**
     * 计算行为得分
     */
    private double getBehaviorScore(Integer behaviorType,int navigate_time) {
        switch (behaviorType) {
            case 3: return 1.0; // 收藏
            case 2: return 0.7; // 点赞
            case 1: return 0.3*(1+0.1*(1.0*navigate_time/600)); // 浏览时间越长，权重越大
            default: return 0.1;
        }
    }

    /**
     * 计算时间衰减因子
     */
    private double calculateTimeDecay(UserPostBehavior behavior, Long currentTime) {
        // 假设行为时间是以秒为单位的时间戳
        Long behaviorTimestamp = behavior.getBehavior_time().atZone(ZoneId.systemDefault()).toInstant().getEpochSecond(); // 秒
        Long timeDiff = currentTime - behaviorTimestamp;

        // 使用指数衰减：半衰期为30天
        double halfLife = 30 * 24 * 60 * 60; // 30天的秒数
        return Math.pow(0.5, timeDiff / halfLife);
    }

    /**
     * 计算用户相似度（余弦相似度）
     */
    private Map<Integer, Double> calculateUserSimilarities(Integer user_id, Map<Integer, Map<Integer, Double>> userPostMatrix) {
        //传入当前用户id和所有用户的用户-帖子矩阵
        Map<Integer, Double> similarities = new HashMap<>();//保存其他用户ID-相似度值映射（只有相似度值大于阈值才添加）
        Map<Integer, Double> targetUserVector = userPostMatrix.getOrDefault(user_id, new HashMap<>());//获取当前用户的帖子行为

        //遍历每个其他用户的行为
        for (Integer otheruser_id : userPostMatrix.keySet()) {
            if (otheruser_id.equals(user_id)) continue;//跳过自己

            Map<Integer, Double> otherUserVector = userPostMatrix.get(otheruser_id);
            double similarity = calculateCosineSimilarity(targetUserVector, otherUserVector);//计算两个用户的余弦相似度
            if (similarity > 0.1) { // 设置相似度阈值
                similarities.put(otheruser_id, similarity);
            }
        }

        //按相似度降序排序
        return similarities.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(100) // 限制相似用户数量
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));
    }

    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(Map<Integer, Double> vectorA, Map<Integer, Double> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        // 计算点积和范数
        for (Integer itemId : vectorA.keySet()) {
            //有共同的行为帖子时
            if (vectorB.containsKey(itemId)) {
                dotProduct += vectorA.get(itemId) * vectorB.get(itemId);
            }
            normA += Math.pow(vectorA.get(itemId), 2);
        }

        for (Double value : vectorB.values()) {
            normB += Math.pow(value, 2);
        }

        if (normA == 0 || normB == 0) return 0.0;

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 生成基于用户的推荐结果
     */
    private List<Post> generateUserBasedRecommendations(Integer user_id,
                                                        Map<Integer, Double> userSimilarities,
                                                        Map<Integer, Map<Integer, Double>> userPostMatrix,
                                                        Integer limit) {
        Map<Integer, Double> postScores = new HashMap<>();
        Map<Integer, Double> postWeights = new HashMap<>();

        Map<Integer, Double> targetUserItems = userPostMatrix.getOrDefault(user_id, new HashMap<>());//当前用户行为

        // 基于相似用户的喜好计算帖子得分（userSimilarities里存的是用户ID-相似度）
        for (Map.Entry<Integer, Double> entry : userSimilarities.entrySet()) {
            Integer similaruser_id = entry.getKey();//用户ID
            Double similarity = entry.getValue();//相似用户与当前用户的相似度

            Map<Integer, Double> similarUserItems = userPostMatrix.get(similaruser_id);//该用户行为

            for (Map.Entry<Integer, Double> itemEntry : similarUserItems.entrySet()) {
                //获取与相似用户有关的帖子和对应分数
                Integer postId = itemEntry.getKey();
                Double score = itemEntry.getValue();

                // 跳过用户已经接触过的帖子
                if (targetUserItems.containsKey(postId)) continue;
                //计算相似用户的有关帖子得分，推荐给当前用户（相似度越高得分越高，越容易推荐）
                postScores.put(postId, postScores.getOrDefault(postId, 0.0) + score * similarity);
                postWeights.put(postId, postWeights.getOrDefault(postId, 0.0) + similarity);
            }
        }

        // 计算加权平均分
        Map<Integer, Double> finalScores = new HashMap<>();
        for (Integer postId : postScores.keySet()) {
            if (postWeights.get(postId) > 0) {
                finalScores.put(postId, postScores.get(postId) / postWeights.get(postId));
            }
        }

        // 按得分降序排序并获取帖子详情
        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> postMapper.selectPostById(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 构建帖子相似度矩阵
     */
    private Map<Integer, Map<Integer, Double>> buildPostSimilarityMatrix(List<UserPostBehavior> behaviors) {
        // 传入的是所有的用户行为
        // 首先构建用户-帖子矩阵
        Map<Integer, Map<Integer, Double>> userPostMatrix = buildUserPostMatrixWithTimeDecay(behaviors);

        // 然后计算帖子相似度
        Map<Integer, Map<Integer, Double>> postSimilarityMatrix = new HashMap<>();
        List<Integer> allPostIds = getAllPostIds(behaviors);

        // 可能需要优化性能
        for (int i = 0; i < allPostIds.size(); i++) {
            Integer postA = allPostIds.get(i);
            for (int j = i + 1; j < allPostIds.size(); j++) {
                Integer postB = allPostIds.get(j);

                double similarity = calculatePostSimilarity(postA, postB, userPostMatrix);
                if (similarity > 0) {
                    //构造相似度矩阵（对称矩阵，其中的值是相似度）
                    postSimilarityMatrix.putIfAbsent(postA, new HashMap<>());
                    postSimilarityMatrix.putIfAbsent(postB, new HashMap<>());
                    postSimilarityMatrix.get(postA).put(postB, similarity);
                    postSimilarityMatrix.get(postB).put(postA, similarity);
                }
            }
        }
        return postSimilarityMatrix;
    }

    /**
     * 计算帖子相似度（基于共同用户）
     */
    private double calculatePostSimilarity(Integer postA, Integer postB, Map<Integer, Map<Integer, Double>> userPostMatrix) {
        Set<Integer> commonUsers = new HashSet<>();
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (Map.Entry<Integer, Map<Integer, Double>> userEntry : userPostMatrix.entrySet()) {
            Map<Integer, Double> userVector = userEntry.getValue();//某用户帖子-得分的映射
            boolean hasA = userVector.containsKey(postA);
            boolean hasB = userVector.containsKey(postB);
            //如果某用户同时跟这两个帖子都有互动
            if (hasA && hasB) {
                commonUsers.add(userEntry.getKey());
                double scoreA = userVector.get(postA);
                double scoreB = userVector.get(postB);
                dotProduct += scoreA * scoreB;
            }

            if (hasA) normA += Math.pow(userVector.get(postA), 2);
            if (hasB) normB += Math.pow(userVector.get(postB), 2);
        }

        if (normA == 0 || normB == 0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 获取所有帖子ID
     */
    private List<Integer> getAllPostIds(List<UserPostBehavior> behaviors) {
        return behaviors.stream()
                .map(UserPostBehavior::getPost_id)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 生成基于物品的推荐结果
     */
    private List<Post> generateItemBasedRecommendations(List<UserPostBehavior> userBehaviors,
                                                        Map<Integer, Map<Integer, Double>> postSimilarityMatrix,
                                                        Integer limit) {
        Map<Integer, Double> postScores = new HashMap<>();
        Set<Integer> userInteractedPosts = userBehaviors.stream()
                .map(UserPostBehavior::getPost_id)
                .collect(Collectors.toSet());

        // 对于用户交互过的每个帖子，找到相似帖子并计算得分
        for (UserPostBehavior behavior : userBehaviors) {
            Integer interactedPostId = behavior.getPost_id();//交互的帖子ID
            int navigate_time=behavior.getNavigate_time();
            double behaviorWeight = getBehaviorScore(behavior.getBehavior_type(),navigate_time);//行为权重，收藏权重最高

            // 获取与该帖子相似的帖子
            Map<Integer, Double> similarPosts = postSimilarityMatrix.getOrDefault(interactedPostId, new HashMap<>());

            for (Map.Entry<Integer, Double> similarEntry : similarPosts.entrySet()) {
                Integer similarPostId = similarEntry.getKey();
                Double similarity = similarEntry.getValue();

                // 跳过用户已经交互过的帖子
                if (userInteractedPosts.contains(similarPostId)) continue;

                double score = behaviorWeight * similarity;
                postScores.put(similarPostId, postScores.getOrDefault(similarPostId, 0.0) + score);
            }
        }

        // 按得分排序并获取帖子详情
        return postScores.entrySet().stream()
                .sorted(Map.Entry.<Integer, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> postMapper.selectPostById(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
