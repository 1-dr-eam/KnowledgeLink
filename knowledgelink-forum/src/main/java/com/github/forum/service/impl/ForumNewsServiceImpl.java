package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.common.dto.Result;
import com.github.forum.entity.ForumNews;
import com.github.forum.mapper.ForumNewsMapper;
import com.github.forum.service.IForumNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ForumNewsServiceImpl implements IForumNewsService {
    @Autowired
    private ForumNewsMapper forumNewsMapper;

    @Override
    public Result getNewsList(Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 10 : limit;
        List<ForumNews> newsList = forumNewsMapper.selectList(new LambdaQueryWrapper<ForumNews>()
                .orderByDesc(ForumNews::getPublishDate)
                .last("limit " + safeLimit));
        return newsList.isEmpty() ? Result.error("获取新闻列表失败") : Result.success(newsList);
    }

    @Override
    public Result getNewsDetail(Long id) {
        ForumNews news = forumNewsMapper.selectById(id);
        return news == null ? Result.error("获取新闻详情失败") : Result.success(news);
    }
}
