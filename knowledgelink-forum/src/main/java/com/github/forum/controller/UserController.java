package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IForumUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private IForumUserService forumUserService;

    @RequestMapping("/login")
    public Result login(String username, String password) {
        return forumUserService.login(username, password);
    }

    @RequestMapping("/register")
    public Result register(String phone, String username, String password, String confirmPassword, String grade, String major) {
        return forumUserService.register(phone, username, password, confirmPassword, grade, major);
    }

    @RequestMapping("/checkUsername")
    public Result checkUsername(String username) {
        return forumUserService.checkUsername(username);
    }

    @RequestMapping("/checkPhone")
    public Result checkPhone(String phone) {
        return forumUserService.checkPhone(phone);
    }

    @RequestMapping("/getUserInfo")
    public Result getUserInfo() {
        return forumUserService.getUserInfo();
    }

    @RequestMapping("/updateUser")
    public Result updateUser(String username, String grade, String major, String summary) {
        return forumUserService.updateUser(username, grade, major, summary);
    }

    @RequestMapping("/likePost")
    public Result likePost(Long postId) {
        return forumUserService.likePost(postId);
    }

    @RequestMapping("/collectPost")
    public Result collectPost(Long postId) {
        return forumUserService.collectPost(postId);
    }

    @RequestMapping("/likeComment")
    public Result likeComment(Long commentId) {
        return forumUserService.likeComment(commentId);
    }

    @RequestMapping("/focusUser")
    public Result focusUser(Long focusUserId) {
        return forumUserService.focusUser(focusUserId);
    }

    @RequestMapping("/cancelLikePost")
    public Result cancelLikePost(Long postId) {
        return forumUserService.cancelLikePost(postId);
    }

    @RequestMapping("/cancelCollectPost")
    public Result cancelCollectPost(Long postId) {
        return forumUserService.cancelCollectPost(postId);
    }

    @RequestMapping("/cancelLikeComment")
    public Result cancelLikeComment(Long commentId) {
        return forumUserService.cancelLikeComment(commentId);
    }

    @RequestMapping("/cancelFocusUser")
    public Result cancelFocusUser(Long focusUserId) {
        return forumUserService.cancelFocusUser(focusUserId);
    }

    @RequestMapping("/checkFocusStatus")
    public Result checkFocusStatus(Long checkUserId) {
        return forumUserService.checkFocusStatus(checkUserId);
    }

    @RequestMapping("/getPostLikeStatus")
    public Result getPostLikeStatus(Long postId) {
        return forumUserService.getPostLikeStatus(postId);
    }

    @RequestMapping("/getPostCollectStatus")
    public Result getPostCollectStatus(Long postId) {
        return forumUserService.getPostCollectStatus(postId);
    }

    @RequestMapping("/getOutstandingCreator")
    public Result getOutstandingCreator() {
        return forumUserService.getOutstandingCreator();
    }

    @PostMapping("/uploadAvatar")
    public Result uploadAvatar(@RequestParam MultipartFile image) {
        return forumUserService.uploadAvatar(image);
    }
}
