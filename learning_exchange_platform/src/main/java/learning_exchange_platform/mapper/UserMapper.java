package learning_exchange_platform.mapper;

import io.swagger.v3.oas.models.security.SecurityScheme;
import learning_exchange_platform.model.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    public boolean insertUser(User user);
    public boolean updateUser(String username,String grade,String major,String summary,int id);
    public boolean setUserAvatar(int id,String avatar);
    public boolean deleteUser(int id);
    public User selectUserById(int id);
    public List<User> selectUsersByIds(@Param("ids") List<Integer> ids);
    public User selectUserByUsername(String username);
    public User selectUserByPassword(String password);
    public User login(String username, String password);
    //以下关于点赞收藏关注等用户操作
    //增删
    public boolean insertLikePost(LikePost likePost);
    public boolean insertCollectPost(CollectPost collectPost);
    public boolean insertLikeComment(LikeComment likeComment);
    public boolean insertFocus(Focus focus);
    public boolean deleteLikePost(int user_id,int post_id);
    public boolean deleteCollectPost(int user_id,int post_id);
    public boolean deleteLikeComment(int user_id,int commment_id);
    public boolean deleteFocus(int user_id,int focus_user_id);
    //查询
    public List<Integer> selectFocusIds(int user_id);
    public List<Integer> selectFansIds(int user_id);

    List<UserProfile> selectOutstandingCreator();
    List<User> getUsersByIds(List<Integer> ids);//保证前后列表顺序不变

    boolean selectPostLikeStatus(int post_id,int user_id);

    boolean selectPostCollectStatus(int post_id,int user_id);

    boolean checkUsername(String username);

    boolean checkPhone(String phone);

    boolean checkFocusStatus(int check_user_id, int user_id);
    //websocket
    List<User> websocket_selectUsersByIds(List<Integer> user_ids);

    boolean selectFriendStatus(int user_id,int focus_user_id);

    boolean postCollectsInc(int post_id);
    boolean postLikesInc(int post_id);
    boolean postCommentsInc(int post_id);

    Integer selectFocusUserCounts(int user_id);

    Integer selectFansUserCounts(int user_id);

    List<Post> searchCollectPosts(int user_id,String searchKey);
}
