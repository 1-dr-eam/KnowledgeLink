package learning_exchange_platform.controller;

import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.PostTitle;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.service.PostService;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PostController {
    @Autowired
    private PostService postService;

    @RequestMapping("/insertPost")
    public Result insertPost(String title, String summary, String content, String cover_avatar, String label, String type, String visible_range,String subject,String sub_classify){
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=postService.insertPost(user_id,title,summary,content,cover_avatar,label,type,visible_range,subject,sub_classify);
        if(success){
            return Result.success();
        }else {
            return Result.error("发布帖子失败");
        }
    }

    @RequestMapping("/deletePost")
    public Result deletePost(int id){
        boolean success=postService.deletePost(id);
        if(success){
            return Result.success();
        }else {
            return Result.error("删除帖子失败");
        }
    }

    @RequestMapping("/updatePost")
    public Result updatePost(int id,String title, String summary, String content, String cover_avatar, String labels, String type, String visible_range,String subject,String sub_classify){
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=postService.updatePost(id,user_id,title,summary,content,cover_avatar,labels,type,visible_range,subject,sub_classify);
        if(success){
            return Result.success();
        }else {
            return Result.error("更新帖子失败");
        }
    }

    @RequestMapping("/getPostsByUserId")
    public Result getPostsByUserId(){
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        List<Post> postList=postService.getPostsByUserId(user_id);
        if(!postList.isEmpty()){
            return Result.success(postList);
        }else {
            return Result.error("查询用户所发帖子失败");
        }
    }

    @RequestMapping("/getUserPostsById")
    public Result getUserPostsById(int userId,String collation,String searchKey){
        try {
            List<Post> postList=postService.getUserPostsById(userId,collation,searchKey);
            return Result.success(postList);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取用户帖子失败");
        }

    }

    @RequestMapping("/getAllPosts")
    public Result getAllPosts(String keyword,String collation,String label,String classify){
        List<Post> posts=postService.getAllPosts(keyword,collation,label,classify);
        if(!posts.isEmpty()){
            return Result.success(posts);
        }else {
            return Result.error("查询帖子失败");
        }
    }

    @RequestMapping("/getLoginUserPosts")
    public Result getLoginUserPosts(String collation){
        try {
            HttpSession session= SessionUtil.getSession();
            List<Post> posts=postService.getLoginUserPosts(session,collation);
            return Result.success(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取帖子失败");
        }
    }

    @RequestMapping("/getLoginUserCollectPosts")
    public Result getLoginUserCollectPosts(){
        try {
            HttpSession session= SessionUtil.getSession();
            int user_id=(int)session.getAttribute("user_id");
            List<Post> posts=postService.getCollectedPostsByUserId(user_id);
            return Result.success(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取帖子收藏夹失败");
        }

    }

    @RequestMapping("/getPostInfoById")
    public Result getPostInfoById(int post_id){
        Post post=postService.selectPostById(post_id);
        if(post!=null){
            return Result.success(post);
        }else {
            return Result.error("获取帖子信息失败");
        }
    }

    @RequestMapping("/incPageViewsById")
    public Result incPageViewsById(int id){
        boolean success=postService.incPageViewsById(id);
        if(success){
            return Result.success();
        }else {
            return Result.error("浏览量自增失败");
        }
    }

    @RequestMapping("/getPostLabelById")
    public Result getPostLabelById(int id){
        String label=postService.selectPostLabelById(id);
        if(label!=null){
            return Result.success(label);
        }else {
            return Result.error("获取帖子话题失败");
        }
    }

    //获取热门话题
    @RequestMapping("/getPopularTopic")
    public Result getPopularTopic(){
        List<PostTitle> popular_topics=postService.getPopularTopic();
        if(!popular_topics.isEmpty()){
            return Result.success(popular_topics);
        }else {
            return Result.error("获取热门话题失败");
        }
    }

    //获取全部帖子的label
    @RequestMapping("/getPostLabels")
    public Result getPostLabels(){
        List<String> labels=postService.getPostLabels();
        if(!labels.isEmpty()){
            return Result.success(labels);
        }else {
            return Result.error("获取帖子label失败");
        }
    }

    //获取全部帖子的子分类
    @RequestMapping("/getPostClassifies")
    public Result getPostClassifies(){
        List<String> classifies=postService.getPostClassifies();
        if(!classifies.isEmpty()){
            return Result.success(classifies);
        }else {
            return Result.error("获取帖子分类失败");
        }
    }

    //获取相似帖子标题
    @RequestMapping("/getTopicSimilarPosts")
    public Result getTopicSimilarPosts(int id,String subject,String sub_classify){
        try {
            List<PostTitle> titles=postService.getTopicSimilarPosts(id,subject,sub_classify);
            return Result.success(titles);
        }catch(Exception e) {
            e.printStackTrace();
            return Result.error("获取相似帖子失败");
        }
    }

}
