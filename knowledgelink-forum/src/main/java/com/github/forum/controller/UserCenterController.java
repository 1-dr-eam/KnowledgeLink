package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IForumUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/userCenter")
public class UserCenterController {
    @Autowired
    private IForumUserService forumUserService;

    @RequestMapping("/getUserCenterInfoById")
    public Result getUserCenterInfoById(Long userId) {
        return forumUserService.getUserCenterInfoById(userId);
    }

    @RequestMapping("/getLikeCounts")
    public Result getLikeCounts() {
        return forumUserService.getLikeCounts();
    }

    @RequestMapping("/getCollectCounts")
    public Result getCollectCounts() {
        return forumUserService.getCollectCounts();
    }

    @RequestMapping("/getPageViews")
    public Result getPageViews() {
        return forumUserService.getPageViews();
    }

    @RequestMapping("/getFocusUser")
    public Result getFocusUser() {
        return forumUserService.getFocusUser();
    }

    @RequestMapping("/getFans")
    public Result getFans() {
        return forumUserService.getFans();
    }

    @RequestMapping("/getFriends")
    public Result getFriends() {
        return forumUserService.getFriends();
    }

    @RequestMapping("/searchCollectPosts")
    public Result searchCollectPosts(String searchKey) {
        return forumUserService.searchCollectPosts(searchKey);
    }
}
