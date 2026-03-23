package com.github.forum.service;

import com.github.common.dto.Result;

public interface IForumNewsService {
    Result getNewsList(Integer limit);
    Result getNewsDetail(Long id);
}
