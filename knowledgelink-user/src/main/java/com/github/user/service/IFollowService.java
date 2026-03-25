package com.github.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.common.dto.Result;
import com.github.user.dto.IdRequest;
import com.github.user.entity.UserFollow;

/**
 * iFollow服务
 *
 * @author ning
 * @date 2026/03/25
 */
public interface IFollowService extends IService<UserFollow> {
    Result follow(IdRequest idRequest);

    Result unfollow(IdRequest idRequest);

    Result getFollowList();

    Result getFollowersList();

    Result getFriendList();

    Result isFollow(IdRequest idRequest);
}
