package com.github.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.common.dto.JwtUserClaimsDTO;
import com.github.common.dto.Result;
import com.github.common.dto.UserDTO;
import com.github.common.utils.JwtTokenUtil;
import com.github.common.utils.RedisConstant;
import com.github.common.utils.UserHolder;
import com.github.user.dto.LoginDTO;
import com.github.user.dto.UserInfoDTO;
import com.github.user.entity.User;
import com.github.user.entity.UserFollow;
import com.github.user.entity.UserProfileBehavior;
import com.github.user.mapper.UserFollowMapper;
import com.github.user.mapper.UserMapper;
import com.github.user.mapper.UserProfileBehaviorMapper;
import com.github.user.service.IUserService;
import com.github.user.vo.UserDashboardVO;
import com.github.user.vo.UserFollowStatVO;
import com.github.user.vo.UserPreferenceBehaviorVO;
import com.github.user.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 用户服务实现类
 *
 * @author ning
 * @date 2026/03/25
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserFollowMapper userFollowMapper;
    private final UserProfileBehaviorMapper userProfileBehaviorMapper;
    private final RestTemplate restTemplate;
    private final String tradeBaseUrl;
    private final String forumBaseUrl;

    public UserServiceImpl(StringRedisTemplate stringRedisTemplate,
                           JwtTokenUtil jwtTokenUtil,
                           UserFollowMapper userFollowMapper,
                           UserProfileBehaviorMapper userProfileBehaviorMapper,
                           RestTemplate restTemplate,
                           @Value("${remote.trade-base-url:http://127.0.0.1:8081/trade}") String tradeBaseUrl,
                           @Value("${remote.forum-base-url:http://127.0.0.1:8085/forum}") String forumBaseUrl) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtTokenUtil = jwtTokenUtil;
        this.userFollowMapper = userFollowMapper;
        this.userProfileBehaviorMapper = userProfileBehaviorMapper;
        this.restTemplate = restTemplate;
        this.tradeBaseUrl = tradeBaseUrl;
        this.forumBaseUrl = forumBaseUrl;
    }

    /**
     * 用户注册
     *
     * @param user 用户实体
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result registerUser(User user) {
        if (user == null || user.getPhone() == null || user.getPassword() == null) {
            return Result.error("注册参数错误");
        }
        User existUser = baseMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, user.getPhone()));
        if (existUser != null) {
            return Result.error("手机号已注册");
        }
        user.setStatus(1);
        baseMapper.insert(user);
        return Result.success();
    }

    /**
     * 用户登录
     *
     * @param loginDTO 登录参数
     * @return 登录结果
     */
    @Override
    public Result loginUser(LoginDTO loginDTO) {
        if (loginDTO == null || loginDTO.getPhone() == null || loginDTO.getPassword() == null) {
            return Result.error("登录参数错误");
        }
        User user = baseMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, loginDTO.getPhone()));
        if (user == null || !loginDTO.getPassword().equals(user.getPassword())) {
            return Result.error("手机号或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            return Result.error("用户已禁用");
        }
        JwtUserClaimsDTO jwtUserClaimsDTO = new JwtUserClaimsDTO();
        jwtUserClaimsDTO.setUserId(user.getId());
        jwtUserClaimsDTO.setPhone(user.getPhone());
        jwtUserClaimsDTO.setUsername(user.getUsername());
        jwtUserClaimsDTO.setMajor(user.getMajor());
        jwtUserClaimsDTO.setGrade(user.getGrade());
        String token = jwtTokenUtil.generateToken(jwtUserClaimsDTO);
        String tokenId = jwtTokenUtil.getTokenId(token);
        jwtTokenUtil.saveTokenSession(tokenId, LocalDateTime.now());
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        cacheUserInfo(userDTO);
        return Result.success(token);
    }

    /**
     * 用户退出登录
     *
     * @return 处理结果
     */
    @Override
    public Result logoutUser() {
        String token = getAuthorizationToken();
        if (token == null || token.isBlank()) {
            return Result.error("未登录");
        }
        if (!jwtTokenUtil.isTokenValid(token)) {
            return Result.error("登录状态无效");
        }
        String tokenId = jwtTokenUtil.getTokenId(token);
        jwtTokenUtil.removeTokenSession(tokenId);
        return Result.success();
    }

    /**
     * 更新用户信息
     *
     * @param userInfoDTO 用户信息参数
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updateUser(UserInfoDTO userInfoDTO) {
        if (userInfoDTO == null) {
            return Result.error("更新参数错误");
        }
        Long userId = UserHolder.getUser().getId();
        User user = baseMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        if (userInfoDTO.getUsername() != null) {
            user.setUsername(userInfoDTO.getUsername());
        }
        if (userInfoDTO.getMajor() != null) {
            user.setMajor(userInfoDTO.getMajor());
        }
        if (userInfoDTO.getGrade() != null) {
            user.setGrade(userInfoDTO.getGrade());
        }
        if (userInfoDTO.getAvatar() != null) {
            user.setAvatar(userInfoDTO.getAvatar());
        }
        if (userInfoDTO.getSummary() != null) {
            user.setSummary(userInfoDTO.getSummary());
        }
        baseMapper.updateById(user);
        deleteUserCache(userId);
        return Result.success();
    }

    /**
     * 删除当前用户
     *
     * @return 处理结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteUser() {
        Long userId = UserHolder.getUser().getId();
        deleteUserCache(userId);
        baseMapper.deleteById(userId);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        deleteUserCache(userId);
        return Result.success();
    }

    /**
     * 查询当前用户信息
     *
     * @return 用户信息
     */
    @Override
    public Result getCurrentUser() {
        Long userId = UserHolder.getUser().getId();
        UserDTO userDTO = getUserFromCache(userId);
        if (userDTO != null) {
            return Result.success(userDTO);
        }
        User user = baseMapper.selectById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }
        userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        cacheUserInfo(userDTO);
        return Result.success(userDTO);
    }

    /**
     * 按ID查询用户信息
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Override
    public Result getUserInfoById(Long id) {
        if (id == null) {
            return Result.error("用户ID不能为空");
        }
        UserDTO userDTO = getUserFromCache(id);
        if (userDTO != null) {
            UserProfileVO userProfileVO = BeanUtil.copyProperties(userDTO, UserProfileVO.class);
            return Result.success(userProfileVO);
        }
        User user = baseMapper.selectById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        cacheUserInfo(userDTO);
        UserProfileVO userProfileVO = BeanUtil.copyProperties(userDTO, UserProfileVO.class);
        return Result.success(userProfileVO);
    }

    /**
     * 按ID查询用户关注统计
     *
     * @param id 用户ID
     * @return 关注统计
     */
    @Override
    public Result getUserFollowStatById(Long id) {
        if (id == null) {
            return Result.error("用户ID不能为空");
        }
        return Result.success(buildUserFollowStat(id));
    }

    @Override
    public Result getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        Set<Long> idSet = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id != null) {
                idSet.add(id);
            }
        }
        if (idSet.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<User> users = baseMapper.selectByIds(new ArrayList<>(idSet));
        List<UserDTO> userDTOList = users.stream().map(user -> {
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
            cacheUserInfo(userDTO);
            return userDTO;
        }).toList();
        return Result.success(userDTOList);
    }

    @Override
    public Result getUserPreferenceBehavior(Long userId) {
        Long targetUserId = userId;
        if (targetUserId == null) {
            if (UserHolder.getUser() == null || UserHolder.getUser().getId() == null) {
                return Result.error("用户未登录");
            }
            targetUserId = UserHolder.getUser().getId();
        }
        UserProfileBehavior behavior = userProfileBehaviorMapper.selectOne(new LambdaQueryWrapper<UserProfileBehavior>()
                .eq(UserProfileBehavior::getUserId, targetUserId)
                .last("limit 1"));
        if (behavior == null) {
            return Result.success(new UserPreferenceBehaviorVO());
        }
        UserPreferenceBehaviorVO vo = BeanUtil.copyProperties(behavior, UserPreferenceBehaviorVO.class);
        vo.setUserId(targetUserId);
        return Result.success(vo);
    }

    @Override
    public Result getUserDashboard() {
        Long userId = UserHolder.getUser().getId();
        UserDTO currentUser = getUserFromCache(userId);
        if (currentUser == null) {
            User user = baseMapper.selectById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }
            currentUser = BeanUtil.copyProperties(user, UserDTO.class);
            cacheUserInfo(currentUser);
        }
        CompletableFuture<String> addressFuture = CompletableFuture.supplyAsync(() -> fetchDefaultShippingAddress());
        CompletableFuture<int[]> forumFuture = CompletableFuture.supplyAsync(() -> fetchForumStats(userId));
        CompletableFuture<UserFollowStatVO> followFuture = CompletableFuture.supplyAsync(() -> buildUserFollowStat(userId));
        CompletableFuture<Integer> friendFuture = CompletableFuture.supplyAsync(() -> countFriend(userId));
        CompletableFuture.allOf(addressFuture, forumFuture, followFuture, friendFuture).join();
        UserDashboardVO userDashboardVO = new UserDashboardVO();
        userDashboardVO.setUsername(currentUser.getUsername());
        userDashboardVO.setAvatar(currentUser.getAvatar());
        userDashboardVO.setGrade(currentUser.getGrade());
        userDashboardVO.setMajor(currentUser.getMajor());
        userDashboardVO.setShippingAddress(addressFuture.join());
        int[] forumStats = forumFuture.join();
        userDashboardVO.setPostPageViews(forumStats[0]);
        userDashboardVO.setPostLikeCount(forumStats[1]);
        userDashboardVO.setPostCollectCount(forumStats[2]);
        UserFollowStatVO followStatVO = followFuture.join();
        userDashboardVO.setFollowersCount(followStatVO.getFollowersCount());
        userDashboardVO.setFollowCount(followStatVO.getFollowCount());
        userDashboardVO.setFriendCount(friendFuture.join());
        return Result.success(userDashboardVO);
    }

    /**
     * 构建用户缓存键
     *
     * @param userId 用户id
     * @return 字符串
     */
    private String buildUserCacheKey(Long userId) {
        return RedisConstant.USER_INFO_KEY + ":" + userId;
    }

    private void cacheUserInfo(UserDTO userDTO) {
        if (userDTO == null || userDTO.getId() == null) {
            return;
        }
        stringRedisTemplate.opsForValue().set(buildUserCacheKey(userDTO.getId()), JSONUtil.toJsonStr(userDTO), RedisConstant.USER_INFO_TTL, TimeUnit.SECONDS);
    }

    /**
     * 从缓存获取用户
     *
     * @param userId 用户id
     * @return 用户dto
     */
    private UserDTO getUserFromCache(Long userId) {
        String cacheJson = stringRedisTemplate.opsForValue().get(buildUserCacheKey(userId));
        if (cacheJson == null || cacheJson.isBlank()) {
            return null;
        }
        return JSONUtil.toBean(cacheJson, UserDTO.class);
    }

    /**
     * 删除用户缓存
     *
     * @param userId 用户id
     */
    private void deleteUserCache(Long userId) {
        stringRedisTemplate.delete(buildUserCacheKey(userId));
    }

    private UserFollowStatVO buildUserFollowStat(Long userId) {
        List<UserFollow> userFollowList = listUserFollowRelations(userId);
        int followCount = 0;
        int followersCount = 0;
        for (UserFollow userFollow : userFollowList) {
            if (userFollow.getStatus() == UserFollow.Status.EACH_FOLLOW) {
                followCount++;
                followersCount++;
                continue;
            }
            if (userFollow.getStatus() == UserFollow.Status.FIRST_FOLLOW) {
                if (userId.equals(userFollow.getFirstUserId())) {
                    followCount++;
                } else {
                    followersCount++;
                }
                continue;
            }
            if (userFollow.getStatus() == UserFollow.Status.SECOND_FOLLOW) {
                if (userId.equals(userFollow.getSecondUserId())) {
                    followCount++;
                } else {
                    followersCount++;
                }
            }
        }
        UserFollowStatVO userFollowStatVO = new UserFollowStatVO();
        userFollowStatVO.setFollowCount(followCount);
        userFollowStatVO.setFollowersCount(followersCount);
        return userFollowStatVO;
    }

    private int countFriend(Long userId) {
        List<UserFollow> userFollowList = listUserFollowRelations(userId);
        int friendCount = 0;
        for (UserFollow userFollow : userFollowList) {
            if (userFollow.getStatus() == UserFollow.Status.EACH_FOLLOW) {
                friendCount++;
            }
        }
        return friendCount;
    }

    private List<UserFollow> listUserFollowRelations(Long userId) {
        return userFollowMapper.selectList(new LambdaQueryWrapper<UserFollow>()
                .eq(UserFollow::getFirstUserId, userId)
                .or()
                .eq(UserFollow::getSecondUserId, userId));
    }

    private String fetchDefaultShippingAddress() {
        try {
            HttpHeaders headers = new HttpHeaders();
            String token = getAuthorizationToken();
            if (token != null && !token.isBlank()) {
                headers.set("Authorization", "Bearer " + token);
            }
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    tradeBaseUrl + "/address/list",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );
            if (responseEntity.getBody() == null) {
                return "";
            }
            JSONObject resultObj = JSONUtil.parseObj(responseEntity.getBody());
            if (resultObj.getInt("code", 0) != 1) {
                return "";
            }
            JSONArray dataArray = resultObj.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                return "";
            }
            JSONObject targetAddress = null;
            for (Object item : dataArray) {
                JSONObject addressObj = JSONUtil.parseObj(item);
                if (Boolean.TRUE.equals(addressObj.getBool("isDefault"))) {
                    targetAddress = addressObj;
                    break;
                }
            }
            if (targetAddress == null) {
                targetAddress = JSONUtil.parseObj(dataArray.get(0));
            }
            return buildAddressText(targetAddress);
        } catch (Exception e) {
            return "";
        }
    }

    private int[] fetchForumStats(Long userId) {
        int[] stats = new int[]{0, 0, 0};
        try {
            ResponseEntity<String> responseEntity = restTemplate.getForEntity(
                    forumBaseUrl + "/post/getByUserId?userId=" + userId,
                    String.class
            );
            if (responseEntity.getBody() == null) {
                return stats;
            }
            JSONObject resultObj = JSONUtil.parseObj(responseEntity.getBody());
            if (resultObj.getInt("code", 0) != 1) {
                return stats;
            }
            JSONArray dataArray = resultObj.getJSONArray("data");
            if (dataArray == null || dataArray.isEmpty()) {
                return stats;
            }
            for (Object item : dataArray) {
                JSONObject forumObj = JSONUtil.parseObj(item);
                stats[0] += forumObj.getInt("pageViews", 0);
                stats[1] += forumObj.getInt("likeCount", 0);
                stats[2] += forumObj.getInt("collectCount", 0);
            }
            return stats;
        } catch (Exception e) {
            return stats;
        }
    }

    private String buildAddressText(JSONObject addressObj) {
        if (addressObj == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        appendAddressPart(builder, addressObj.getStr("province"));
        appendAddressPart(builder, addressObj.getStr("city"));
        appendAddressPart(builder, addressObj.getStr("district"));
        appendAddressPart(builder, addressObj.getStr("detailAddress"));
        return builder.toString();
    }

    private void appendAddressPart(StringBuilder builder, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(value);
    }

    /**
     * 获取授权令牌
     *
     * @return 字符串
     */
    private String getAuthorizationToken() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        HttpServletRequest request = requestAttributes.getRequest();
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}

