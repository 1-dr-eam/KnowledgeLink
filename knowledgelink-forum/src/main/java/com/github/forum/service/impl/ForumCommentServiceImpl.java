package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.common.dto.Result;
import com.github.common.dto.UserDTO;
import com.github.common.utils.UserHolder;
import com.github.forum.entity.ForumComment;
import com.github.forum.entity.ForumReplyComment;
import com.github.forum.mapper.ForumCommentMapper;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.mapper.ForumReplyCommentMapper;
import com.github.forum.service.IForumCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ForumCommentServiceImpl implements IForumCommentService {
    @Autowired
    private ForumCommentMapper forumCommentMapper;
    @Autowired
    private ForumReplyCommentMapper forumReplyCommentMapper;
    @Autowired
    private ForumPostMapper forumPostMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result insertComment(String commentType, Long commentId, String replyType, Long postId, Long replyCommentId, String content) {
        UserDTO userDTO = UserHolder.getUser();
        if ("comment".equals(commentType)) {
            ForumComment comment = new ForumComment();
            comment.setPostId(postId);
            comment.setUserId(userDTO.getId());
            comment.setUserName(userDTO.getUsername());
            comment.setContent(content);
            comment.setReplyCount(0);
            comment.setCreateTime(LocalDateTime.now());
            comment.setUpdateTime(LocalDateTime.now());
            forumCommentMapper.insert(comment);
            forumPostMapper.update(null, new LambdaUpdateWrapper<com.github.forum.entity.ForumPost>()
                    .eq(com.github.forum.entity.ForumPost::getId, postId)
                    .setSql("comment_count = ifnull(comment_count,0) + 1"));
            return Result.success();
        }
        if ("reply".equals(commentType)) {
            ForumReplyComment replyComment = new ForumReplyComment();
            replyComment.setPostId(postId);
            replyComment.setCommentId(commentId);
            replyComment.setReplyType(replyType);
            replyComment.setReplyCommentId(replyCommentId);
            replyComment.setUserId(userDTO.getId());
            replyComment.setUserName(userDTO.getUsername());
            replyComment.setContent(content);
            replyComment.setCreateTime(LocalDateTime.now());
            replyComment.setUpdateTime(LocalDateTime.now());
            if ("comment".equals(replyType)) {
                ForumComment parent = forumCommentMapper.selectById(replyCommentId);
                replyComment.setReplyUserName(parent == null ? "" : parent.getUserName());
                if (parent != null) {
                    parent.setReplyCount((parent.getReplyCount() == null ? 0 : parent.getReplyCount()) + 1);
                    parent.setUpdateTime(LocalDateTime.now());
                    forumCommentMapper.updateById(parent);
                }
            } else {
                ForumReplyComment parent = forumReplyCommentMapper.selectById(replyCommentId);
                replyComment.setReplyUserName(parent == null ? "" : parent.getUserName());
            }
            forumReplyCommentMapper.insert(replyComment);
            forumPostMapper.update(null, new LambdaUpdateWrapper<com.github.forum.entity.ForumPost>()
                    .eq(com.github.forum.entity.ForumPost::getId, postId)
                    .setSql("comment_count = ifnull(comment_count,0) + 1"));
            return Result.success();
        }
        return Result.error("评论失败");
    }

    @Override
    public Result getCommentsByPostId(Long postId) {
        List<ForumComment> comments = forumCommentMapper.selectList(new LambdaQueryWrapper<ForumComment>()
                .eq(ForumComment::getPostId, postId)
                .orderByDesc(ForumComment::getCreateTime));
        return Result.success(comments);
    }

    @Override
    public Result getRepliesByCommentId(Long commentId) {
        List<ForumReplyComment> replies = forumReplyCommentMapper.selectList(new LambdaQueryWrapper<ForumReplyComment>()
                .eq(ForumReplyComment::getCommentId, commentId)
                .orderByDesc(ForumReplyComment::getCreateTime));
        return Result.success(replies);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteCommentById(String commentType, Long commentId) {
        if ("comment".equals(commentType)) {
            ForumComment comment = forumCommentMapper.selectById(commentId);
            if (comment == null) {
                return Result.success();
            }
            forumCommentMapper.deleteById(commentId);
            forumReplyCommentMapper.delete(new LambdaQueryWrapper<ForumReplyComment>().eq(ForumReplyComment::getCommentId, commentId));
            forumPostMapper.update(null, new LambdaUpdateWrapper<com.github.forum.entity.ForumPost>()
                    .eq(com.github.forum.entity.ForumPost::getId, comment.getPostId())
                    .setSql("comment_count = if(ifnull(comment_count,0) > 0, comment_count - 1, 0)"));
            return Result.success();
        }
        if ("reply".equals(commentType)) {
            Set<Long> ids = new HashSet<>();
            Queue<Long> queue = new LinkedList<>();
            ids.add(commentId);
            queue.offer(commentId);
            while (!queue.isEmpty()) {
                Long current = queue.poll();
                List<Long> childIds = forumReplyCommentMapper.selectList(new LambdaQueryWrapper<ForumReplyComment>()
                                .eq(ForumReplyComment::getReplyCommentId, current))
                        .stream().map(ForumReplyComment::getId).toList();
                for (Long childId : childIds) {
                    if (ids.add(childId)) {
                        queue.offer(childId);
                    }
                }
            }
            if (!ids.isEmpty()) {
                forumReplyCommentMapper.deleteBatchIds(ids);
            }
            return Result.success();
        }
        return Result.error("删评失败");
    }
}
