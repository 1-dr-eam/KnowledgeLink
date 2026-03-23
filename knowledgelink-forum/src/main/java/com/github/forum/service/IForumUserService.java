package com.github.forum.service;

import com.github.common.dto.Result;
import org.springframework.web.multipart.MultipartFile;

public interface IForumUserService {
    Result login(String username, String password);
    Result register(String phone, String username, String password, String confirmPassword, String grade, String major);
    Result getUserInfo();
    Result getUserCenterInfoById(Long userId);
    Result updateUser(String username, String grade, String major, String summary);
    Result likePost(Long postId);
    Result collectPost(Long postId);
    Result likeComment(Long commentId);
    Result focusUser(Long focusUserId);
    Result cancelLikePost(Long postId);
    Result cancelCollectPost(Long postId);
    Result cancelLikeComment(Long commentId);
    Result cancelFocusUser(Long focusUserId);
    Result getOutstandingCreator();
    Result getPostLikeStatus(Long postId);
    Result getPostCollectStatus(Long postId);
    Result checkUsername(String username);
    Result checkPhone(String phone);
    Result checkFocusStatus(Long checkUserId);
    Result getLikeCounts();
    Result getCollectCounts();
    Result getPageViews();
    Result getFocusUser();
    Result getFans();
    Result getFriends();
    Result searchCollectPosts(String searchKey);
    Result uploadAvatar(MultipartFile image);
}
