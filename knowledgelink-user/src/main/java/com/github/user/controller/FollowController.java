package com.github.user.controller;

import com.github.common.dto.Result;
import com.github.user.dto.IdRequest;
import com.github.user.entity.UserFollow;
import com.github.user.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ning
 * @date 2026-03-25
 */
@RestController
public class FollowController {
    @Autowired
    private IFollowService followService;

    @PostMapping("/follow")
    public Result follow(@RequestBody IdRequest idRequest) {
        return followService.follow(idRequest);
    }

    @PostMapping("/unfollow")
    public Result unfollow(@RequestBody IdRequest idRequest) {
        return followService.unfollow(idRequest);
    }

    @GetMapping("/getFollowList")
    public Result getFollowList() {
        return followService.getFollowList();
    }

    @GetMapping("/getFollowersList")
    public Result getFollowersList() {
        return followService.getFollowersList();
    }

    @GetMapping("/getFriendList")
    public Result getFriendList() {
        return followService.getFriendList();
    }

    @GetMapping("/isFollow")
    public Result isFollow(@RequestParam("id") Long id) {
        IdRequest idRequest = new IdRequest();
        idRequest.setId(id);
        return followService.isFollow(idRequest);
    }
}

