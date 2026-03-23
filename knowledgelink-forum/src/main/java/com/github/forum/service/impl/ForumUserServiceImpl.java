package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.common.dto.JwtUserClaimsDTO;
import com.github.common.dto.Result;
import com.github.common.dto.UserDTO;
import com.github.common.utils.CosUtil;
import com.github.common.utils.JwtTokenUtil;
import com.github.common.utils.UserHolder;
import com.github.forum.entity.*;
import com.github.forum.mapper.*;
import com.github.forum.service.IForumUserService;
import com.github.forum.vo.UserCenterInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class ForumUserServiceImpl implements IForumUserService {
    @Autowired
    private ForumUserMapper forumUserMapper;
    @Autowired
    private ForumPostMapper forumPostMapper;
    @Autowired
    private ForumPostLikeMapper forumPostLikeMapper;
    @Autowired
    private ForumPostCollectMapper forumPostCollectMapper;
    @Autowired
    private ForumCommentLikeMapper forumCommentLikeMapper;
    @Autowired
    private ForumUserFollowMapper forumUserFollowMapper;
    @Autowired
    private JwtTokenUtil jwtTokenUtil;
    @Autowired
    private CosUtil cosUtil;

    @Override
    public Result login(String username, String password) {
        ForumUser user = forumUserMapper.selectOne(new LambdaQueryWrapper<ForumUser>()
                .eq(ForumUser::getUsername, username)
                .eq(ForumUser::getPassword, encryptPassword(password)));
        if (user == null) {
            return Result.error("用户名或密码错误");
        }
        JwtUserClaimsDTO claimsDTO = new JwtUserClaimsDTO();
        claimsDTO.setUserId(user.getId());
        claimsDTO.setPhone(user.getPhone());
        claimsDTO.setUsername(user.getUsername());
        claimsDTO.setMajor(user.getMajor());
        claimsDTO.setGrade(user.getGrade());
        String token = jwtTokenUtil.generateToken(claimsDTO);
        jwtTokenUtil.saveTokenSession(jwtTokenUtil.getTokenId(token), LocalDateTime.now());
        return Result.success(token);
    }

    @Override
    public Result register(String phone, String username, String password, String confirmPassword, String grade, String major) {
        if (!Objects.equals(password, confirmPassword)) {
            return Result.error("两次密码不一致");
        }
        if (Boolean.TRUE.equals((Boolean) checkUsername(username).getData())) {
            return Result.error("用户名已存在");
        }
        if (Boolean.TRUE.equals((Boolean) checkPhone(phone).getData())) {
            return Result.error("手机号已存在");
        }
        ForumUser user = new ForumUser();
        user.setPhone(phone);
        user.setUsername(username);
        user.setPassword(encryptPassword(password));
        user.setMajor(major);
        user.setGrade(grade);
        user.setAvatar("");
        user.setSummary("");
        user.setBalance(0D);
        forumUserMapper.insert(user);
        JwtUserClaimsDTO claimsDTO = new JwtUserClaimsDTO();
        claimsDTO.setUserId(user.getId());
        claimsDTO.setPhone(user.getPhone());
        claimsDTO.setUsername(user.getUsername());
        claimsDTO.setMajor(user.getMajor());
        claimsDTO.setGrade(user.getGrade());
        String token = jwtTokenUtil.generateToken(claimsDTO);
        jwtTokenUtil.saveTokenSession(jwtTokenUtil.getTokenId(token), LocalDateTime.now());
        return Result.success(token);
    }

    @Override
    public Result getUserInfo() {
        Long userId = getCurrentUserId();
        ForumUser user = forumUserMapper.selectById(userId);
        return user == null ? Result.error("获取当前用户信息失败") : Result.success(user);
    }

    @Override
    public Result getUserCenterInfoById(Long userId) {
        ForumUser user = forumUserMapper.selectById(userId);
        if (user == null) {
            return Result.error("获取其他用户信息失败");
        }
        UserCenterInfoVO infoVO = buildUserCenterInfo(userId, user);
        return Result.success(infoVO);
    }

    @Override
    public Result updateUser(String username, String grade, String major, String summary) {
        Long userId = getCurrentUserId();
        boolean updated = forumUserMapper.update(null, new LambdaUpdateWrapper<ForumUser>()
                .eq(ForumUser::getId, userId)
                .set(username != null, ForumUser::getUsername, username)
                .set(grade != null, ForumUser::getGrade, grade)
                .set(major != null, ForumUser::getMajor, major)
                .set(summary != null, ForumUser::getSummary, summary)) > 0;
        return updated ? Result.success("个人资料修改成功") : Result.error("个人资料修改失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result likePost(Long postId) {
        Long userId = getCurrentUserId();
        if (existsPostLike(userId, postId)) {
            return Result.success();
        }
        ForumPostLike like = new ForumPostLike();
        like.setUserId(userId);
        like.setPostId(postId);
        like.setCreateTime(LocalDateTime.now());
        forumPostLikeMapper.insert(like);
        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .setSql("like_count = ifnull(like_count,0) + 1"));
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result collectPost(Long postId) {
        Long userId = getCurrentUserId();
        if (existsPostCollect(userId, postId)) {
            return Result.success();
        }
        ForumPostCollect collect = new ForumPostCollect();
        collect.setUserId(userId);
        collect.setPostId(postId);
        collect.setCreateTime(LocalDateTime.now());
        forumPostCollectMapper.insert(collect);
        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .setSql("collect_count = ifnull(collect_count,0) + 1"));
        return Result.success();
    }

    @Override
    public Result likeComment(Long commentId) {
        Long userId = getCurrentUserId();
        boolean exists = forumCommentLikeMapper.selectCount(new LambdaQueryWrapper<ForumCommentLike>()
                .eq(ForumCommentLike::getUserId, userId)
                .eq(ForumCommentLike::getCommentId, commentId)) > 0;
        if (!exists) {
            ForumCommentLike like = new ForumCommentLike();
            like.setUserId(userId);
            like.setCommentId(commentId);
            like.setCreateTime(LocalDateTime.now());
            forumCommentLikeMapper.insert(like);
        }
        return Result.success();
    }

    @Override
    public Result focusUser(Long focusUserId) {
        Long userId = getCurrentUserId();
        boolean exists = forumUserFollowMapper.selectCount(new LambdaQueryWrapper<ForumUserFollow>()
                .eq(ForumUserFollow::getUserId, userId)
                .eq(ForumUserFollow::getFollowUserId, focusUserId)) > 0;
        if (!exists) {
            ForumUserFollow follow = new ForumUserFollow();
            follow.setUserId(userId);
            follow.setFollowUserId(focusUserId);
            follow.setCreateTime(LocalDateTime.now());
            forumUserFollowMapper.insert(follow);
        }
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelLikePost(Long postId) {
        Long userId = getCurrentUserId();
        forumPostLikeMapper.delete(new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getUserId, userId)
                .eq(ForumPostLike::getPostId, postId));
        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .setSql("like_count = if(ifnull(like_count,0) > 0, like_count - 1, 0)"));
        return Result.success();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result cancelCollectPost(Long postId) {
        Long userId = getCurrentUserId();
        forumPostCollectMapper.delete(new LambdaQueryWrapper<ForumPostCollect>()
                .eq(ForumPostCollect::getUserId, userId)
                .eq(ForumPostCollect::getPostId, postId));
        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, postId)
                .setSql("collect_count = if(ifnull(collect_count,0) > 0, collect_count - 1, 0)"));
        return Result.success();
    }

    @Override
    public Result cancelLikeComment(Long commentId) {
        Long userId = getCurrentUserId();
        forumCommentLikeMapper.delete(new LambdaQueryWrapper<ForumCommentLike>()
                .eq(ForumCommentLike::getUserId, userId)
                .eq(ForumCommentLike::getCommentId, commentId));
        return Result.success();
    }

    @Override
    public Result cancelFocusUser(Long focusUserId) {
        Long userId = getCurrentUserId();
        forumUserFollowMapper.delete(new LambdaQueryWrapper<ForumUserFollow>()
                .eq(ForumUserFollow::getUserId, userId)
                .eq(ForumUserFollow::getFollowUserId, focusUserId));
        return Result.success();
    }

    @Override
    public Result getOutstandingCreator() {
        List<ForumPost> postList = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>());
        Map<Long, Double> scoreMap = new HashMap<>();
        for (ForumPost post : postList) {
            double score = post.getPageViews() / 50.0 * 0.2 + post.getLikeCount() * 0.2 + post.getCollectCount() * 0.3 + post.getCommentCount() * 0.2 + 1.0;
            scoreMap.merge(post.getUserId(), score, Double::sum);
        }
        List<Long> userIds = scoreMap.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
        return Result.success(userIds.isEmpty() ? new ArrayList<>() : forumUserMapper.selectBatchIds(userIds));
    }

    @Override
    public Result getPostLikeStatus(Long postId) {
        return Result.success(existsPostLike(getCurrentUserId(), postId));
    }

    @Override
    public Result getPostCollectStatus(Long postId) {
        return Result.success(existsPostCollect(getCurrentUserId(), postId));
    }

    @Override
    public Result checkUsername(String username) {
        boolean exists = forumUserMapper.selectCount(new LambdaQueryWrapper<ForumUser>().eq(ForumUser::getUsername, username)) > 0;
        return Result.success(exists);
    }

    @Override
    public Result checkPhone(String phone) {
        boolean exists = forumUserMapper.selectCount(new LambdaQueryWrapper<ForumUser>().eq(ForumUser::getPhone, phone)) > 0;
        return Result.success(exists);
    }

    @Override
    public Result checkFocusStatus(Long checkUserId) {
        Long userId = getCurrentUserId();
        boolean isFocus = forumUserFollowMapper.selectCount(new LambdaQueryWrapper<ForumUserFollow>()
                .eq(ForumUserFollow::getUserId, userId)
                .eq(ForumUserFollow::getFollowUserId, checkUserId)) > 0;
        return Result.success(isFocus);
    }

    @Override
    public Result getLikeCounts() {
        Long userId = getCurrentUserId();
        int count = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId))
                .stream().mapToInt(post -> post.getLikeCount() == null ? 0 : post.getLikeCount()).sum();
        return Result.success(count);
    }

    @Override
    public Result getCollectCounts() {
        Long userId = getCurrentUserId();
        int count = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId))
                .stream().mapToInt(post -> post.getCollectCount() == null ? 0 : post.getCollectCount()).sum();
        return Result.success(count);
    }

    @Override
    public Result getPageViews() {
        Long userId = getCurrentUserId();
        int count = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId))
                .stream().mapToInt(post -> post.getPageViews() == null ? 0 : post.getPageViews()).sum();
        return Result.success(count);
    }

    @Override
    public Result getFocusUser() {
        Long userId = getCurrentUserId();
        List<Long> ids = forumUserFollowMapper.selectList(new LambdaQueryWrapper<ForumUserFollow>()
                        .eq(ForumUserFollow::getUserId, userId))
                .stream().map(ForumUserFollow::getFollowUserId).toList();
        return Result.success(ids.isEmpty() ? new ArrayList<>() : forumUserMapper.selectBatchIds(ids));
    }

    @Override
    public Result getFans() {
        Long userId = getCurrentUserId();
        List<Long> ids = forumUserFollowMapper.selectList(new LambdaQueryWrapper<ForumUserFollow>()
                        .eq(ForumUserFollow::getFollowUserId, userId))
                .stream().map(ForumUserFollow::getUserId).toList();
        return Result.success(ids.isEmpty() ? new ArrayList<>() : forumUserMapper.selectBatchIds(ids));
    }

    @Override
    public Result getFriends() {
        Long userId = getCurrentUserId();
        Set<Long> focusSet = new HashSet<>(forumUserFollowMapper.selectList(new LambdaQueryWrapper<ForumUserFollow>()
                .eq(ForumUserFollow::getUserId, userId)).stream().map(ForumUserFollow::getFollowUserId).toList());
        Set<Long> fanSet = new HashSet<>(forumUserFollowMapper.selectList(new LambdaQueryWrapper<ForumUserFollow>()
                .eq(ForumUserFollow::getFollowUserId, userId)).stream().map(ForumUserFollow::getUserId).toList());
        focusSet.retainAll(fanSet);
        return Result.success(focusSet.isEmpty() ? new ArrayList<>() : forumUserMapper.selectBatchIds(focusSet));
    }

    @Override
    public Result searchCollectPosts(String searchKey) {
        Long userId = getCurrentUserId();
        List<Long> postIds = forumPostCollectMapper.selectList(new LambdaQueryWrapper<ForumPostCollect>()
                        .eq(ForumPostCollect::getUserId, userId))
                .stream().map(ForumPostCollect::getPostId).toList();
        if (postIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<ForumPost> postList = forumPostMapper.selectBatchIds(postIds).stream()
                .filter(post -> post.getTitle() != null && post.getTitle().contains(searchKey == null ? "" : searchKey))
                .sorted((a, b) -> Integer.compare(b.getPageViews() == null ? 0 : b.getPageViews(), a.getPageViews() == null ? 0 : a.getPageViews()))
                .toList();
        return Result.success(postList);
    }

    @Override
    public Result uploadAvatar(MultipartFile image) {
        try {
            Long userId = getCurrentUserId();
            String url = cosUtil.uploadImage(image);
            forumUserMapper.update(null, new LambdaUpdateWrapper<ForumUser>()
                    .eq(ForumUser::getId, userId)
                    .set(ForumUser::getAvatar, url));
            return Result.success(url);
        } catch (Exception e) {
            return Result.error("文件上传失败");
        }
    }

    private UserCenterInfoVO buildUserCenterInfo(Long userId, ForumUser user) {
        UserCenterInfoVO infoVO = new UserCenterInfoVO();
        infoVO.setUsername(user.getUsername());
        infoVO.setAvatar(user.getAvatar());
        infoVO.setSummary(user.getSummary());
        infoVO.setGrade(user.getGrade());
        infoVO.setMajor(user.getMajor());
        infoVO.setFocusCount((int) forumUserFollowMapper.selectCount(new LambdaQueryWrapper<ForumUserFollow>().eq(ForumUserFollow::getUserId, userId)));
        infoVO.setFansCount((int) forumUserFollowMapper.selectCount(new LambdaQueryWrapper<ForumUserFollow>().eq(ForumUserFollow::getFollowUserId, userId)));
        List<ForumPost> postList = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId));
        infoVO.setPostCount(postList.size());
        infoVO.setLikeCount(postList.stream().mapToInt(post -> post.getLikeCount() == null ? 0 : post.getLikeCount()).sum());
        infoVO.setCollectCount(postList.stream().mapToInt(post -> post.getCollectCount() == null ? 0 : post.getCollectCount()).sum());
        infoVO.setViewCount(postList.stream().mapToInt(post -> post.getPageViews() == null ? 0 : post.getPageViews()).sum());
        return infoVO;
    }

    private boolean existsPostLike(Long userId, Long postId) {
        return forumPostLikeMapper.selectCount(new LambdaQueryWrapper<ForumPostLike>()
                .eq(ForumPostLike::getUserId, userId)
                .eq(ForumPostLike::getPostId, postId)) > 0;
    }

    private boolean existsPostCollect(Long userId, Long postId) {
        return forumPostCollectMapper.selectCount(new LambdaQueryWrapper<ForumPostCollect>()
                .eq(ForumPostCollect::getUserId, userId)
                .eq(ForumPostCollect::getPostId, postId)) > 0;
    }

    private Long getCurrentUserId() {
        UserDTO userDTO = UserHolder.getUser();
        return userDTO == null ? null : userDTO.getId();
    }

    private String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return password;
        }
    }
}
