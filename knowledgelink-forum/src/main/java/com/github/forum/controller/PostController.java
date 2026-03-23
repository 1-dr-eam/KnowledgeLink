package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IForumPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PostController {
    @Autowired
    private IForumPostService forumPostService;

    @RequestMapping("/insertPost")
    public Result insertPost(String title, String summary, String content, String cover_avatar, String label, String type, String visible_range, String subject, String sub_classify) {
        return forumPostService.insertPost(title, summary, content, cover_avatar, label, type, visible_range, subject, sub_classify);
    }

    @RequestMapping("/deletePost")
    public Result deletePost(Long id) {
        return forumPostService.deletePost(id);
    }

    @RequestMapping("/updatePost")
    public Result updatePost(Long id, String title, String summary, String content, String cover_avatar, String labels, String type, String visible_range, String subject, String sub_classify) {
        return forumPostService.updatePost(id, title, summary, content, cover_avatar, labels, type, visible_range, subject, sub_classify);
    }

    @RequestMapping("/getPostsByUserId")
    public Result getPostsByUserId() {
        return forumPostService.getPostsByUserId();
    }

    @RequestMapping("/getUserPostsById")
    public Result getUserPostsById(Long userId, String collation, String searchKey) {
        return forumPostService.getUserPostsById(userId, collation, searchKey);
    }

    @RequestMapping("/getAllPosts")
    public Result getAllPosts(String keyword, String collation, String label, String classify) {
        return forumPostService.getAllPosts(keyword, collation, label, classify);
    }

    @RequestMapping("/getLoginUserPosts")
    public Result getLoginUserPosts(String collation) {
        return forumPostService.getLoginUserPosts(collation);
    }

    @RequestMapping("/getLoginUserCollectPosts")
    public Result getLoginUserCollectPosts() {
        return forumPostService.getLoginUserCollectPosts();
    }

    @RequestMapping("/getPostInfoById")
    public Result getPostInfoById(Long post_id) {
        return forumPostService.getPostInfoById(post_id);
    }

    @RequestMapping("/incPageViewsById")
    public Result incPageViewsById(Long id) {
        return forumPostService.incPageViewsById(id);
    }

    @RequestMapping("/getPostLabelById")
    public Result getPostLabelById(Long id) {
        return forumPostService.getPostLabelById(id);
    }

    @RequestMapping("/getPopularTopic")
    public Result getPopularTopic() {
        return forumPostService.getPopularTopic();
    }

    @RequestMapping("/getPostLabels")
    public Result getPostLabels() {
        return forumPostService.getPostLabels();
    }

    @RequestMapping("/getPostClassifies")
    public Result getPostClassifies() {
        return forumPostService.getPostClassifies();
    }

    @RequestMapping("/getTopicSimilarPosts")
    public Result getTopicSimilarPosts(Long id, String subject, String sub_classify) {
        return forumPostService.getTopicSimilarPosts(id, subject, sub_classify);
    }
}
