package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.dto.ForumUpsertDTO;
import com.github.forum.dto.IdRequest;
import com.github.forum.dto.SearchDTO;
import com.github.forum.service.IForumPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 帖子控制器
 *
 * @author ning
 * @date 2026/03/24
 */

@RestController
@RequestMapping("/post")
public class PostController {
    @Autowired
    private IForumPostService forumPostService;

    @PostMapping("/add")
    public Result addPost(@RequestBody ForumUpsertDTO forumUpsertDTO) {
        return forumPostService.addPost(forumUpsertDTO);
    }

    @PutMapping("/update")
    public Result updatePost(@RequestParam("id") String id, @RequestBody ForumUpsertDTO forumUpsertDTO) {
        IdRequest idRequest = new IdRequest();
        idRequest.setId(id);
        return forumPostService.updatePost(idRequest, forumUpsertDTO);
    }

    @DeleteMapping("/delete")
    public Result deletePost(@RequestBody IdRequest idRequest) {
        return forumPostService.deletePost(idRequest);
    }

    @PostMapping("/getById")
    public Result getPostById(@RequestBody IdRequest idRequest) {
        return forumPostService.getPostById(idRequest);
    }

    @GetMapping("/getByUserId")
    public Result getPostsByUserId(@RequestParam("userId") String userId) {
        return forumPostService.getPostsByUserId(userId);
    }

    @GetMapping("/collects")
    public Result getMyCollectedPosts() {
        return forumPostService.getMyCollectedPosts();
    }

    @PostMapping("/detail")
    public Result getPostDetailById(@RequestBody IdRequest idRequest) {
        return forumPostService.getPostDetailById(idRequest);
    }

    @PostMapping("/search")
    public Result searchPosts(@RequestBody SearchDTO searchDTO) {
        return forumPostService.searchPosts(searchDTO);
    }

    @GetMapping("/recommended")
    public Result getRecommendedPosts(@RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return forumPostService.getRecommendedPosts(limit);
    }

    @GetMapping("/hot")
    public Result getHotPosts(@RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return forumPostService.getHotPosts(limit);
    }

    @GetMapping("/hotTopics")
    public Result getHotTopics(@RequestParam(value = "limit", required = false, defaultValue = "12") Integer limit) {
        return forumPostService.getHotTopics(limit);
    }

    @GetMapping("/excellentCreators")
    public Result getExcellentCreators(@RequestParam(value = "limit", required = false, defaultValue = "10") Integer limit) {
        return forumPostService.getExcellentCreators(limit);
    }
}
