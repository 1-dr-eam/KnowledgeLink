package learning_exchange_platform.service;

import learning_exchange_platform.mapper.PostMapper;
import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class UserService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private PostMapper postMapper;

    public User login(String username, String password) {
        User user=userMapper.login(username, password);
        return user;
    }

    public boolean register(User user) {
        try {
            userMapper.insertUser(user);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public User getUserInfo(int id){
        return userMapper.selectUserById(id);
    }

    //给某帖子点赞
    public boolean likePost(int user_id,int post_id){
        try {
            LikePost likePost=new LikePost();
            User user=userMapper.selectUserById(user_id);
            Post post=postMapper.selectPostById(post_id);

            likePost.setUser_id(user_id);
            likePost.setPost_id(post_id);
            likePost.setUsername(user.getUsername());
            likePost.setUser_avatar(user.getAvatar());
            likePost.setPost_title(post.getTitle());
            likePost.setLike_date(LocalDate.now());

            userMapper.insertLikePost(likePost);
            //帖子点赞数自增1
            userMapper.postLikesInc(post_id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //收藏某帖子
    public boolean collectPost(int user_id,int post_id){
        try {
            CollectPost collectPost=new CollectPost();
            User user=userMapper.selectUserById(user_id);
            Post post=postMapper.selectPostById(post_id);

            collectPost.setUser_id(user_id);
            collectPost.setPost_id(post_id);
            collectPost.setUsername(user.getUsername());
            collectPost.setUser_avatar(user.getAvatar());
            collectPost.setPost_title(post.getTitle());
            collectPost.setCollect_date(LocalDate.now());

            userMapper.insertCollectPost(collectPost);
            //帖子收藏量自增1
            userMapper.postCollectsInc(post_id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean likeComment(int user_id,int comment_id) {
        try {
            LikeComment likeComment=new LikeComment();

            likeComment.setUser_id(user_id);
            likeComment.setComment_id(comment_id);

            userMapper.insertLikeComment(likeComment);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean focusUser(int user_id,int focus_user_id) {
        try {
            Focus focus=new Focus();

            focus.setUser_id1(user_id);
            focus.setUser_id2(focus_user_id);

            userMapper.insertFocus(focus);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancelLikePost(int user_id,int post_id){
        return userMapper.deleteLikePost(user_id,post_id);
    }

    public boolean cancelCollectPost(int user_id,int post_id){
        return userMapper.deleteCollectPost(user_id,post_id);
    }

    public boolean cancelLikeComment(int user_id,int commment_id){
        return userMapper.deleteLikeComment(user_id,commment_id);
    }

    public boolean cancelFocus(int user_id,int focus_user_id){
        return userMapper.deleteFocus(user_id, focus_user_id);
    }

    public int getLikeCounts(int user_id) {
        try {
            List<Post> posts=postMapper.selectPostsByUserId(user_id);
            int count=0;
            for(Post post:posts){
                count+=post.getLike_count();
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }

    }

    public int getCollectCounts(int user_id) {
        try {
            List<Post> posts=postMapper.selectPostsByUserId(user_id);
            int count=0;
            for(Post post:posts){
                count+=post.getCollect_count();
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getPageViews(int user_id) {
        try {
            List<Post> posts=postMapper.selectPostsByUserId(user_id);
            int count=0;
            for(Post post:posts){
                count+=post.getPage_views();
            }
            return count;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public List<User> getFocusUser(int user_id) {
        try {
            List<Integer> ids=userMapper.selectFocusIds(user_id);
            if(ids==null||ids.isEmpty()) {
                return null;
            }else {
                List<User> users=userMapper.selectUsersByIds(ids);
                return users;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<User> getFans(int user_id) {
        try {
            List<Integer> ids=userMapper.selectFansIds(user_id);
            if(ids==null||ids.isEmpty()) {
                return null;
            }else {
                List<User> users=userMapper.selectUsersByIds(ids);
                return users;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<User> getFriends(int user_id) {
        try {
            List<Integer> focus_ids=userMapper.selectFocusIds(user_id);
            List<Integer> fans_ids=userMapper.selectFansIds(user_id);
            List<Integer> friends_ids=new ArrayList<>(focus_ids);
            friends_ids.retainAll(fans_ids);
            List<User> users=userMapper.selectUsersByIds(friends_ids);
            return users;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<User> getOutstandingCreator() {
        try {
            List<UserProfile> userProfiles=userMapper.selectOutstandingCreator();
            List<Integer> ids=new ArrayList<>();
            for(UserProfile userProfile:userProfiles){
                ids.add(userProfile.getUser_id());
            }
            List<User> users = userMapper.selectUsersByIds(ids);

            // 创建ID到User的映射（快速查找）
            Map<Integer, User> userMap = new HashMap<>();
            for (User user : users) {
                userMap.put(user.getId(), user);
            }

            // 按照传入ID的顺序重新排列
            List<User> orderedUsers = new ArrayList<>();
            for (Integer id : ids) {
                User user = userMap.get(id);
                if (user != null) {
                    orderedUsers.add(user);
                }
            }
            return orderedUsers;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean getPostLikeStatus(int post_id,int user_id) {
        return userMapper.selectPostLikeStatus(post_id,user_id);
    }

    public boolean getPostCollectStatus(int post_id,int user_id) {
        return userMapper.selectPostCollectStatus(post_id,user_id);
    }

    public boolean checkUsername(String username) {
        return userMapper.checkUsername(username);
    }

    public boolean checkPhone(String phone) {
        return userMapper.checkPhone(phone);
    }

    public boolean checkFocusStatus(int check_user_id, int user_id) {
        return userMapper.checkFocusStatus(check_user_id,user_id);
    }

    public boolean updateUser(String username, String grade, String major, String summary,int id) {
        try {
            userMapper.updateUser(username,grade,major,summary,id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<User> getUserByIds(List<Integer> chatFriendIds) {
        try {
            return userMapper.selectUsersByIds(chatFriendIds);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public User selectUserById(Integer currentUserId) {
        try {
            return userMapper.selectUserById(currentUserId);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean getFriendStatus(int user_id, int focus_user_id) {
        try {
            return userMapper.selectFriendStatus(user_id,focus_user_id);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean postCommentsInc(Integer post_id) {
        try {
            return userMapper.postCommentsInc(post_id);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public UserCenterInfo getUserCenterInfo(int user_id) {
        try {
            User user=userMapper.selectUserById(user_id);
            int focusCount=userMapper.selectFocusUserCounts(user_id);
            int fansCount=userMapper.selectFansUserCounts(user_id);
            int postCount=postMapper.selectPostsByUserId(user_id).size();
            int likeCount=getLikeCounts(user_id);
            int collectCount=getCollectCounts(user_id);
            int viewCount=getPageViews(user_id);
            UserCenterInfo userCenterInfo=new UserCenterInfo(user.getUsername(),user.getAvatar(),user.getSummary(),user.getGrade()
                    ,user.getMajor(),focusCount,fansCount,postCount,likeCount,collectCount,viewCount);
            return userCenterInfo;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Post> searchCollectPosts(int user_id,String searchKey) {
        try {
            return userMapper.searchCollectPosts(user_id,searchKey);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
