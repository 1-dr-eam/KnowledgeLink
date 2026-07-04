package com.github.forum.service;

import com.github.common.dto.Result;
import com.github.forum.dto.ForumUpsertDTO;
import com.github.forum.dto.IdRequest;
import com.github.forum.dto.SearchDTO;

/**
 * IForumPostService 服务接口
 *
 * @author ning
 * @date 2026/03/24
 */

public interface IForumPostService {
    Result addPost(ForumUpsertDTO forumUpsertDTO);
    Result updatePost(IdRequest idRequest, ForumUpsertDTO forumUpsertDTO);
    Result deletePost(IdRequest idRequest);
    Result getPostById(IdRequest idRequest);
    Result getPostsByUserId(String userId);
    Result getMyCollectedPosts();
    Result getPostDetailById(IdRequest idRequest);
    Result searchPosts(SearchDTO searchDTO);
    Result getRecommendedPosts(Integer limit);
    Result getHotPosts(Integer limit);
    Result getHotTopics(Integer limit);
    Result getExcellentCreators(Integer limit);
}
