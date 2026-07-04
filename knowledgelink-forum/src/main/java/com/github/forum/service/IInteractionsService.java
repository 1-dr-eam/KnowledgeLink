package com.github.forum.service;

import com.github.common.dto.Result;

/**
 * interaction服务
 *
 * @author ning
 * @date 2026/04/03
 */
public interface IInteractionsService {
    Result like(Long forumId);

    Result collect(Long forumId);

    Result unlike(Long forumId);

    Result uncollect(Long forumId);
}
