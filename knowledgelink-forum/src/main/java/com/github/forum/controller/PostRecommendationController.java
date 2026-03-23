package com.github.forum.controller;

import com.github.common.dto.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
public class PostRecommendationController {
    @RequestMapping("/getRecommendedPosts")
    public Result getRecommendedPosts(Integer limit) {
        // TODO 推荐逻辑后续补充
        return Result.success(new ArrayList<>());
    }
}
