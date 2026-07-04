package com.github.forum.service;

import com.github.common.dto.Result;

/**
 * IForumNewsService 服务接口
 *
 * @author ning
 * @date 2026/03/24
 */

public interface IForumNewsService {
    Result getNewsList(Integer limit);
    Result getNewsDetail(Long id);
}
