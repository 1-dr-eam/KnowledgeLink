package learning_exchange_platform.mapper;

import learning_exchange_platform.model.Comment;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.PostTitle;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface PostMapper {
    public boolean insertPost(Post post);
    public boolean deletePost(int id);

    public boolean updatePost(Post post);
    public boolean updateCollectTitleById(int post_id, String post_title);
    public boolean updateLikeTitleById(int post_id, String post_title);
    public boolean updatePageViewsById(int id, int page_views);

    public List<Post> selectAllPosts();
    public List<Post> selectAllPostsByTime();
    public List<Post> selectAllPostsByPageViews();
    public Post selectPostById(int id);
    public List<Post> selectPostsByUserId(int user_id);
    public List<Post> selectUserPostsByTime(int user_id);
    public List<Post> selectUserPostsByPageViews(int user_id);
    public List<Post> selectUserPostsByTimeAndKey(int user_id,String searchKey);
    public List<Post> selectUserPostsByPageViewsAndKey(int user_id,String searchKey);
    public List<Post> selectPostsByLabelAndTime(String label);
    public List<Post> selectPostsByLabelAndPageViews(String label);
    public List<Post> selectPostsByLabel(String label);
    public List<Integer> selectCollectedPostsByUserId(int user_id);
    public List<Post> selectPostsByIds(List<Integer> post_ids);
    //推荐系统(寻找内容相似帖子)
    List<Post> selectContentSimilarPosts(String subject,String label,String sub_classify, int id, int limit);
    List<PostTitle> selectTopicSimilarPosts(int id,String subject,String sub_classify);
    List<Post> selectPopularPosts(Integer limit);
    List<Post> selectLatestPosts(Integer limit);

    String selectPostLabelById(int id);

    List<PostTitle> selectPopularTopics();

    List<String> selectPostLabels();

    List<String> selectPostClassifies();

    List<Post> selectPostsBySubClassify(String sub_classify);
}
