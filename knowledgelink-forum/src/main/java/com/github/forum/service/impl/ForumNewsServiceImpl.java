package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.common.dto.Result;
import com.github.forum.entity.Forum;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.service.IForumNewsService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * ForumNewsServiceImpl 服务实现类
 *
 * @author ning
 * @date 2026/03/24
 */

@Service
public class ForumNewsServiceImpl implements IForumNewsService {
    private final ForumPostMapper forumPostMapper;

    public ForumNewsServiceImpl(ForumPostMapper forumPostMapper) {
        this.forumPostMapper = forumPostMapper;
    }
    /**
     * 执行getNewsList逻辑
     *
     * @param limit limit参数
     * @return 处理结果
     */
    @Override
    public Result getNewsList(Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 10 : Math.min(limit, 50);
        List<Forum> forumList = forumPostMapper.selectList(
                new LambdaQueryWrapper<Forum>()
                        .orderByDesc(Forum::getCreateTime)
                        .last("limit " + safeLimit)
        );
        return Result.success(forumList);
    }

    /**
     * 执行getNewsDetail逻辑
     *
     * @param id id参数
     * @return 处理结果
     */
    @Override
    public Result getNewsDetail(Long id) {
        if (id == null) {
            return Result.error("参数异常");
        }
        Forum forum = forumPostMapper.selectById(id);
        if (forum == null) {
            return Result.error("内容不存在");
        }
        return Result.success(forum);
    }
}
