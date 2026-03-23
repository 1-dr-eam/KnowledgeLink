package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IForumNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NewsController {
    @Autowired
    private IForumNewsService forumNewsService;

    @RequestMapping("/getNewsList")
    public Result getNewsList(Integer limit) {
        return forumNewsService.getNewsList(limit);
    }

    @RequestMapping("/getNewsDetail")
    public Result getNewsDetail(Long id) {
        return forumNewsService.getNewsDetail(id);
    }
}
