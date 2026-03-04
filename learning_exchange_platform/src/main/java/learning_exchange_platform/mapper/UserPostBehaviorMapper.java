package learning_exchange_platform.mapper;

import learning_exchange_platform.model.*;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserPostBehaviorMapper {
    // 插入用户行为
    boolean insertUserPostBehavior(UserPostBehavior UserPostBehavior);

    // 更新用户行为（如浏览时间更新）
    boolean updateViewBehavior(int user_id,int post_id,int behavior_time);

    // 获取用户的所有行为
    List<UserPostBehavior> selectPostBehaviorByUserId(int user_id);

    // 获取用户对特定帖子的行为
    List<UserPostBehavior> selectByUserAndPost(int user_id,int post_id);

    // 获取所有用户行为数据用于协同过滤
    List<UserPostBehavior> selectAllUserBehaviors();

    // 获取用户浏览过的帖子（用于内容推荐）
    List<Post> selectUserViewedPosts(int user_id);

    // 获取用户点赞过的帖子
    List<Post> selectUserLikedPosts(int user_id);

    // 获取用户收藏过的帖子
    List<Post> selectUserCollectedPosts(int user_id);

    // 获取相似用户的行为（用于基于用户的协同过滤）
    List<UserPostBehavior> selectSimilarUserBehaviors(int user_id);

    // 获取用户最近的行为（带分页）
    List<UserPostBehavior> selectRecentBehaviors(int user_id,int limit);

    // 统计用户行为数量
    Integer countUserBehaviors(int user_id);

    // 删除用户行为（可选功能）
    Integer deleteUserBehavior(int user_id,int post_id,int behavior_type);

    List<UserPostBehavior> selectNavigateBehaviorByUserId(int user_id);

    List<UserPostBehavior> selectUserPostBehaviorById(int id);
}
