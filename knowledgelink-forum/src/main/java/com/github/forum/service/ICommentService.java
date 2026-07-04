package com.github.forum.service;

import com.github.common.dto.Result;
import com.github.forum.dto.ForumCommentDTO;
import com.github.forum.dto.IdRequest;

/**
 * IForumCommentService 服务接口
 *
 * @author ning
 * @date 2026/03/24
 */

public interface ICommentService {
    Result addComment(ForumCommentDTO forumCommentDTO);
    Result deleteCommentById(IdRequest idRequest);
    Result getLevel1CommentsByForumId(IdRequest idRequest);
    Result getLevel2CommentsByRootCommentId(IdRequest idRequest);
}
