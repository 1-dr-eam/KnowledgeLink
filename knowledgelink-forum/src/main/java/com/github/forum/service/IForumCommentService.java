package com.github.forum.service;

import com.github.common.dto.Result;

public interface IForumCommentService {
    Result insertComment(String commentType, Long commentId, String replyType, Long postId, Long replyCommentId, String content);
    Result getCommentsByPostId(Long postId);
    Result getRepliesByCommentId(Long commentId);
    Result deleteCommentById(String commentType, Long commentId);
}
