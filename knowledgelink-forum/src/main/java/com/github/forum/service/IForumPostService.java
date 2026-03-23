package com.github.forum.service;

import com.github.common.dto.Result;

public interface IForumPostService {
    Result insertPost(String title, String summary, String content, String coverAvatar, String label, String type, String visibleRange, String subject, String subClassify);
    Result deletePost(Long id);
    Result updatePost(Long id, String title, String summary, String content, String coverAvatar, String label, String type, String visibleRange, String subject, String subClassify);
    Result getPostsByUserId();
    Result getUserPostsById(Long userId, String collation, String searchKey);
    Result getAllPosts(String keyword, String collation, String label, String classify);
    Result getLoginUserPosts(String collation);
    Result getLoginUserCollectPosts();
    Result getPostInfoById(Long postId);
    Result incPageViewsById(Long id);
    Result getPostLabelById(Long id);
    Result getPopularTopic();
    Result getPostLabels();
    Result getPostClassifies();
    Result getTopicSimilarPosts(Long id, String subject, String subClassify);
}
