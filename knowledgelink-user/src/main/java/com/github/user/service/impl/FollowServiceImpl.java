package com.github.user.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.Result;
import com.github.common.utils.IdCompareUtil;
import com.github.common.utils.UserHolder;
import com.github.user.dto.IdRequest;
import com.github.user.entity.UserFollow;
import com.github.user.mapper.UserFollowMapper;
import com.github.user.service.IFollowService;
import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.common.utils.RedisConstant.USER_FOLLOW_KEY;
import static com.github.common.utils.RedisConstant.USER_FOLLOW_TTL;

/**
 * 用户关注服务实现类
 *
 * @author ning
 * @date 2026-03-25
 */
@Service
public class FollowServiceImpl extends ServiceImpl<UserFollowMapper, UserFollow> implements IFollowService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private IdCompareUtil idCompareUtil;

    /**
     * 关注用户
     *
     * @param idRequest 目标用户ID参数
     * @return 处理结果
     */
    @Override
    public Result follow(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("关注参数错误");
        }
        Long currId = UserHolder.getUser().getId();
        if (currId.equals(idRequest.getId())) {
            return Result.error("不能关注自己");
        }
        String id = idCompareUtil.idCompare(currId, idRequest.getId());
        UserFollow userFollow = getUserFollow(id);
        if (userFollow == null) {
            UserFollow newFollow = new UserFollow();
            newFollow.setId(id);
            newFollow.setFirstUserId(idCompareUtil.getFirstId(id));
            newFollow.setSecondUserId(idCompareUtil.getSecondId(id));
            if (currId.equals(idCompareUtil.getFirstId(id))) {
                newFollow.setStatus(UserFollow.Status.FIRST_FOLLOW);
            } else {
                newFollow.setStatus(UserFollow.Status.SECOND_FOLLOW);
            }
            baseMapper.insert(newFollow);
            cacheUserFollow(newFollow);
            return Result.success();
        }

        UserFollow.Status followStatus = userFollow.getStatus();
        if (currId.equals(userFollow.getFirstUserId())) {
            if (followStatus == UserFollow.Status.FIRST_FOLLOW || followStatus == UserFollow.Status.EACH_FOLLOW) {
                return Result.success();
            }
            return updateFollowStatus(id, UserFollow.Status.EACH_FOLLOW);
        }
        if (followStatus == UserFollow.Status.SECOND_FOLLOW || followStatus == UserFollow.Status.EACH_FOLLOW) {
            return Result.success();
        }
        return updateFollowStatus(id, UserFollow.Status.EACH_FOLLOW);
    }

    /**
     * 取消关注用户
     *
     * @param idRequest 目标用户ID参数
     * @return 处理结果
     */
    @Override
    public Result unfollow(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("取消关注参数错误");
        }
        Long currId = UserHolder.getUser().getId();
        if (currId.equals(idRequest.getId())) {
            return Result.error("不能取消关注自己");
        }
        String id = idCompareUtil.idCompare(currId, idRequest.getId());
        UserFollow userFollow = getUserFollow(id);
        if (userFollow == null) {
            return Result.success();
        }
        UserFollow.Status followStatus = userFollow.getStatus();
        boolean currentIsFirst = currId.equals(userFollow.getFirstUserId());

        if (followStatus == UserFollow.Status.EACH_FOLLOW) {
            UserFollow.Status targetStatus = currentIsFirst ? UserFollow.Status.SECOND_FOLLOW : UserFollow.Status.FIRST_FOLLOW;
            return updateFollowStatus(id, targetStatus);
        }
        if (followStatus == UserFollow.Status.FIRST_FOLLOW && currentIsFirst) {
            baseMapper.deleteById(id);
            deleteUserFollowCache(id);
            return Result.success();
        }
        if (followStatus == UserFollow.Status.SECOND_FOLLOW && !currentIsFirst) {
            baseMapper.deleteById(id);
            deleteUserFollowCache(id);
            return Result.success();
        }
        return Result.success();
    }

    /**
     * 查询当前用户关注列表
     *
     * @return 关注用户ID列表
     */
    @Override
    public Result getFollowList() {
        Long currId = UserHolder.getUser().getId();
        List<UserFollow> userFollows = listCurrentUserRelations(currId);
        List<Long> followIds = new ArrayList<>();
        for (UserFollow userFollow : userFollows) {
            boolean currentIsFirst = currId.equals(userFollow.getFirstUserId());
            if (userFollow.getStatus() == UserFollow.Status.EACH_FOLLOW) {
                followIds.add(currentIsFirst ? userFollow.getSecondUserId() : userFollow.getFirstUserId());
            } else if (userFollow.getStatus() == UserFollow.Status.FIRST_FOLLOW && currentIsFirst) {
                followIds.add(userFollow.getSecondUserId());
            } else if (userFollow.getStatus() == UserFollow.Status.SECOND_FOLLOW && !currentIsFirst) {
                followIds.add(userFollow.getFirstUserId());
            }
        }
        return Result.success(followIds);
    }

    /**
     * 查询当前用户粉丝列表
     *
     * @return 粉丝用户ID列表
     */
    @Override
    public Result getFollowersList() {
        Long currId = UserHolder.getUser().getId();
        List<UserFollow> userFollows = listCurrentUserRelations(currId);
        List<Long> followerIds = new ArrayList<>();
        for (UserFollow userFollow : userFollows) {
            boolean currentIsFirst = currId.equals(userFollow.getFirstUserId());
            if (userFollow.getStatus() == UserFollow.Status.EACH_FOLLOW) {
                followerIds.add(currentIsFirst ? userFollow.getSecondUserId() : userFollow.getFirstUserId());
            } else if (userFollow.getStatus() == UserFollow.Status.FIRST_FOLLOW && !currentIsFirst) {
                followerIds.add(userFollow.getFirstUserId());
            } else if (userFollow.getStatus() == UserFollow.Status.SECOND_FOLLOW && currentIsFirst) {
                followerIds.add(userFollow.getSecondUserId());
            }
        }
        return Result.success(followerIds);
    }

    /**
     * 查询当前用户互关好友列表
     *
     * @return 互关用户ID列表
     */
    @Override
    public Result getFriendList() {
        Long currId = UserHolder.getUser().getId();
        List<UserFollow> userFollows = listCurrentUserRelations(currId);
        List<Long> friendIds = new ArrayList<>();
        for (UserFollow userFollow : userFollows) {
            if (userFollow.getStatus() != UserFollow.Status.EACH_FOLLOW) {
                continue;
            }
            boolean currentIsFirst = currId.equals(userFollow.getFirstUserId());
            friendIds.add(currentIsFirst ? userFollow.getSecondUserId() : userFollow.getFirstUserId());
        }
        return Result.success(friendIds);
    }

    /**
     * 判断当前用户是否已关注目标用户
     *
     * @param idRequest 目标用户ID参数
     * @return 是否已关注
     */
    @Override
    public Result isFollow(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null) {
            return Result.error("判断关注参数错误");
        }
        Long currId = UserHolder.getUser().getId();
        if (currId.equals(idRequest.getId())) {
            return Result.success(false);
        }
        String id = idCompareUtil.idCompare(currId, idRequest.getId());
        UserFollow userFollow = getUserFollow(id);
        if (userFollow == null) {
            return Result.success(false);
        }
        boolean currentIsFirst = currId.equals(userFollow.getFirstUserId());
        boolean isFollow = userFollow.getStatus() == UserFollow.Status.EACH_FOLLOW
                || (userFollow.getStatus() == UserFollow.Status.FIRST_FOLLOW && currentIsFirst)
                || (userFollow.getStatus() == UserFollow.Status.SECOND_FOLLOW && !currentIsFirst);
        return Result.success(isFollow);
    }

    /**
     * 更新关注状态
     *
     * @param id ID
     * @param status 状态
     * @return 处理结果
     */
    private Result updateFollowStatus(String id, UserFollow.Status status) {
        int rows = baseMapper.update(null, new LambdaUpdateWrapper<UserFollow>()
                .eq(UserFollow::getId, id)
                .set(UserFollow::getStatus, status));
        if (rows <= 0) {
            return Result.error("操作失败");
        }
        deleteUserFollowCache(id);
        UserFollow updated = baseMapper.selectById(id);
        if (updated != null) {
            cacheUserFollow(updated);
        }
        return Result.success();
    }

    /**
     * 获取关注关系
     *
     * @param id 关系ID
     * @return 关注关系
     */
    @Nullable
    private UserFollow getUserFollow(String id) {
        String cacheKey = USER_FOLLOW_KEY + id;
        if (stringRedisTemplate.hasKey(cacheKey)) {
            String userFollowJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (userFollowJson != null && !userFollowJson.isBlank()) {
                return JSONUtil.toBean(userFollowJson, UserFollow.class);
            }
        }
        UserFollow userFollow = baseMapper.selectById(id);
        if (userFollow != null) {
            cacheUserFollow(userFollow);
        }
        return userFollow;
    }

    /**
     * 查询当前用户所有关系记录
     *
     * @param currId 当前用户ID
     * @return 关系列表
     */
    private List<UserFollow> listCurrentUserRelations(Long currId) {
        return baseMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFirstUserId, currId)
                .or()
                .eq(UserFollow::getSecondUserId, currId));
    }

    /**
     * 缓存用户关注
     *
     * @param userFollow 用户关注
     */
    private void cacheUserFollow(UserFollow userFollow) {
        String tokenKey = USER_FOLLOW_KEY + userFollow.getId();
        stringRedisTemplate.opsForValue().set(tokenKey, JSONUtil.toJsonStr(userFollow));
        stringRedisTemplate.expire(tokenKey, USER_FOLLOW_TTL, TimeUnit.SECONDS);
    }

    /**
     * 删除关注关系缓存
     *
     * @param id 关系ID
     */
    private void deleteUserFollowCache(String id) {
        stringRedisTemplate.delete(USER_FOLLOW_KEY + id);
    }
}
