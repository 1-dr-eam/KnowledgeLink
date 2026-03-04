package learning_exchange_platform.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.mapper.PostMapper;
import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.*;
import learning_exchange_platform.utils.PostScoreComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class PostService {
    @Autowired
    private PostMapper postMapper;
    @Autowired
    private UserMapper userMapper;

    public boolean insertPost(int user_id,String title,String summary,String content,String cover_avatar,String label,String type,String visible_range,String subject,String sub_classify){
        try {
            LocalDate publish_date = LocalDate.now();
            LocalDateTime publish_date_time = LocalDateTime.now();
            User user=userMapper.selectUserById(user_id);
            Post post = new Post(user_id,title,publish_date,summary,content,user.getUsername(),user.getAvatar(),label,cover_avatar,type,visible_range);
            post.setSubject(subject);
            post.setSub_classify(sub_classify);
            postMapper.insertPost(post);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deletePost(int id){
        try {
            postMapper.deletePost(id);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updatePost(int id,int user_id,String title,String summary,String content,String cover_avatar,String label,String type,String visible_range,String subject,String sub_classify){
        try {
            User user=userMapper.selectUserById(user_id);
            Post post = new Post(id,user_id,title,summary,content,user.getUsername(),user.getAvatar(),label,cover_avatar,type,visible_range,subject,sub_classify);
            postMapper.updatePost(post);
            postMapper.updateCollectTitleById(post.getId(),title);
            postMapper.updateLikeTitleById(post.getId(),title);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Post selectPostById(int id){
        return postMapper.selectPostById(id);
    }

    public List<Post> getPostsByUserId(int user_id){
        return postMapper.selectPostsByUserId(user_id);
    }

    public List<Post> getUserPostsById(int user_id,String collation,String searchKey){
        try {
            List<Post> posts;
            if(!searchKey.isEmpty()){
                if(collation.equals("time")||collation.isEmpty()){
                    posts=postMapper.selectUserPostsByTimeAndKey(user_id,searchKey);
                }else{
                    posts=postMapper.selectUserPostsByPageViewsAndKey(user_id,searchKey);
                }
            }else {
                if(collation.equals("time")||collation.isEmpty()){
                    posts=postMapper.selectUserPostsByTime(user_id);
                }else{
                    posts=postMapper.selectUserPostsByPageViews(user_id);
                }
            }
            return posts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Post> getAllPosts(String keyword,String collation,String label,String classify){
        try {
            List<Post> posts;
            //根据label筛选
            if(label.isEmpty() && classify.isEmpty()){
                posts=postMapper.selectAllPosts();
            }else if(classify.isEmpty()){
                //classify为空，而label非空
                posts=postMapper.selectPostsByLabel(label);
            }else {
                //label为空，而classify非空
                posts=postMapper.selectPostsBySubClassify(classify);
            }
            //再去除不满足模糊查询的
            List<Post> copy_posts=new ArrayList<>(posts);;
            for(Post post:posts){
                boolean is_contain=StringUtils.containsIgnoreCase(post.getTitle(),keyword);
                if(!is_contain){
                    copy_posts.remove(post);
                }
            }
            //最后根据collation排序
            posts=sortByCollation(copy_posts,collation);
            return posts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //collation为排序规则
    public List<Post> sortByCollation(List<Post> posts,String collation){
        if(collation.equals("综合")||collation.isEmpty()){
            //利用自定义的综合排序规则进行再排序
            posts.sort(new PostScoreComparator());
            return posts;
        }else if(collation.equals("最新")){
            posts.sort(Comparator.comparing(Post::getPublish_date).reversed());
            return posts;
        }else {
            //热门，按浏览量排序
            posts.sort(Comparator.comparing(Post::getPage_views).reversed());
            return posts;
        }
    }

    public List<Post> getLoginUserPosts(HttpSession session,String collation){
        try {
            int user_id=(int)session.getAttribute("user_id");
            List<Post> posts=postMapper.selectPostsByUserId(user_id);
            if(collation==null){
                posts.sort(Comparator.comparing(Post::getPublish_date).reversed());
            }else {
                posts.sort(Comparator.comparing(Post::getPage_views).reversed());
            }
            return posts;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //查询某用户收藏的帖子
    public List<Post> getCollectedPostsByUserId(int user_id){
        try {
            List<Integer> post_ids=postMapper.selectCollectedPostsByUserId(user_id);
            if(post_ids.isEmpty()){
                return null;
            }else {
                return postMapper.selectPostsByIds(post_ids);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean incPageViewsById(int id){
        try {
            Post post=postMapper.selectPostById(id);
            int page_views=post.getPage_views()+1;
            postMapper.updatePageViewsById(id,page_views);
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public String selectPostLabelById(int id) {
        try {
            return postMapper.selectPostLabelById(id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PostTitle> getPopularTopic() {
        try {
            return postMapper.selectPopularTopics();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getPostLabels() {
        try {
            return postMapper.selectPostLabels();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<String> getPostClassifies() {
        try {
            return postMapper.selectPostClassifies();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<PostTitle> getTopicSimilarPosts(int id, String subject, String sub_classify) {
        return postMapper.selectTopicSimilarPosts(id,subject,sub_classify);
    }
}
