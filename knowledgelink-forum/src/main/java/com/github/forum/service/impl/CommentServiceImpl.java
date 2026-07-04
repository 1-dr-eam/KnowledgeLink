package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.forum.dto.ForumCommentDTO;
import com.github.forum.dto.IdRequest;
import com.github.forum.entity.Forum;
import com.github.forum.entity.ForumComment;
import com.github.forum.mapper.CommentMapper;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.service.ICommentService;
import com.github.forum.service.UserRemoteClient;
import com.github.forum.vo.CommentLevel1VO;
import com.github.forum.vo.CommentLevel2VO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * 评论服务实现类
 * 基于 MyBatis-Plus 提供评论新增、删除、一级评论查询、二级评论查询能力。
 * 一级评论规则：parentId = 0，rootId = 自身id，replyToCommentId = 0。
 * 二级评论规则：parentId = 一级评论id，rootId = 一级评论id。
 * 点赞状态与点赞数量在查询 VO 中暂预留，后续补充。
 *
 * @author ning
 * @date 2026/04/02
 */
@Service
public class CommentServiceImpl implements ICommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private ForumPostMapper forumPostMapper;
    @Autowired
    private UserRemoteClient userRemoteClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增评论
     *
     * @param forumCommentDTO 评论参数
     * @return 新增结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addComment(ForumCommentDTO forumCommentDTO) {
        if (forumCommentDTO == null || forumCommentDTO.getForumId() == null || forumCommentDTO.getForumId().isBlank()
                || forumCommentDTO.getContent() == null || forumCommentDTO.getContent().isBlank()) {
            return Result.error("评论参数不能为空");
        }
        Long forumId = parseLongId(forumCommentDTO.getForumId());
        if (forumId == null) {
            return Result.error("帖子ID非法");
        }
        Forum forum = forumPostMapper.selectById(forumId);
        if (forum == null) {
            return Result.error("帖子不存在");
        }
        ForumComment comment = new ForumComment();
        comment.setForumId(forumId);
        comment.setUserId(UserHolder.getUser().getId());
        comment.setContent(forumCommentDTO.getContent());
        comment.setReplyUserName(forumCommentDTO.getReplyUserName());
        comment.setStatus(forumCommentDTO.getStatus() == null ? 1 : forumCommentDTO.getStatus());
        comment.setLikeCount(0);
        comment.setCreateTime(LocalDateTime.now());
        comment.setUpdateTime(LocalDateTime.now());
        Long parentId = parseLongIdOrDefault(forumCommentDTO.getParentId(), 0L);
        if (parentId == 0L) {
            comment.setParentId(0L);
            comment.setRootId(0L);
            comment.setReplyToCommentId(0L);
            commentMapper.insert(comment);
            comment.setRootId(comment.getId());
            commentMapper.updateById(comment);
        } else {
            Long rootId = parseLongIdOrDefault(forumCommentDTO.getRootId(), 0L);
            rootId = rootId == 0L ? parentId : rootId;
            comment.setParentId(rootId);
            comment.setRootId(rootId);
            comment.setReplyToCommentId(parseLongIdOrDefault(forumCommentDTO.getReplyToCommentId(), parentId));
            commentMapper.insert(comment);
        }
        forumPostMapper.update(null, new LambdaUpdateWrapper<Forum>()
                .eq(Forum::getId, forumId)
                .setSql("comment_count = ifnull(comment_count,0) + 1"));
        return Result.success();
    }

    /**
     * 删除评论
     *
     * @param idRequest 评论ID参数
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deleteCommentById(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank()) {
            return Result.error("评论ID不能为空");
        }
        Long commentId = parseLongId(idRequest.getId());
        if (commentId == null) {
            return Result.error("评论ID非法");
        }
        ForumComment target = commentMapper.selectById(commentId);
        if (target == null) {
            return Result.success();
        }
        List<Long> deleteIds = new ArrayList<>();
        deleteIds.add(target.getId());
        if (target.getParentId() != null && target.getParentId() == 0L) {
            List<Long> childIds = commentMapper.selectList(new LambdaQueryWrapper<ForumComment>()
                            .eq(ForumComment::getRootId, target.getId())
                            .ne(ForumComment::getId, target.getId()))
                    .stream().map(ForumComment::getId).toList();
            deleteIds.addAll(childIds);
        }
        commentMapper.deleteBatchIds(deleteIds);
        forumPostMapper.update(null, new LambdaUpdateWrapper<Forum>()
                .eq(Forum::getId, target.getForumId())
                .setSql("comment_count = if(ifnull(comment_count,0) > " + deleteIds.size() + ", comment_count - " + deleteIds.size() + ", 0)"));
        return Result.success();
    }

    /**
     * 查询帖子下所有一级评论
     *
     * @param idRequest 帖子ID参数
     * @return 一级评论列表
     */
    @Override
    public Result getLevel1CommentsByForumId(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank()) {
            return Result.error("帖子ID不能为空");
        }
        Long forumId = parseLongId(idRequest.getId());
        if (forumId == null) {
            return Result.error("帖子ID非法");
        }
        List<ForumComment> comments = commentMapper.selectList(new LambdaQueryWrapper<ForumComment>()
                .eq(ForumComment::getForumId, forumId)
                .eq(ForumComment::getParentId, 0L)
                .orderByDesc(ForumComment::getCreateTime));
        Long currentUserId = UserHolder.getUser().getId();
        Map<Long, UserRemoteClient.UserSimpleInfo> userInfoMap = new HashMap<>();
        List<CommentLevel1VO> voList = comments.stream().map(comment -> {
            CommentLevel1VO vo = new CommentLevel1VO();
            BeanUtils.copyProperties(comment, vo);
            UserRemoteClient.UserSimpleInfo userSimpleInfo = userInfoMap.computeIfAbsent(comment.getUserId(), userRemoteClient::getUserSimpleInfo);
            vo.setUsername(userSimpleInfo.getUsername());
            vo.setAvatar(userSimpleInfo.getAvatar());
            vo.setId(String.valueOf(comment.getId()));
            vo.setUserId(String.valueOf(comment.getUserId()));
            vo.setLikeStatus(Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildCommentLikeKey(currentUserId, comment.getId()))));
            vo.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
            Integer subCommentCount = Math.toIntExact(commentMapper.selectCount(new LambdaQueryWrapper<ForumComment>()
                    .eq(ForumComment::getRootId, comment.getId())
                    .ne(ForumComment::getId, comment.getId())));
            vo.setCommentCount(subCommentCount);
            return vo;
        }).toList();
        return Result.success(voList);
    }

    /**
     * 查询一级评论下所有二级评论
     *
     * @param idRequest 一级评论ID参数
     * @return 二级评论列表
     */
    @Override
    public Result getLevel2CommentsByRootCommentId(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank()) {
            return Result.error("一级评论ID不能为空");
        }
        Long rootCommentId = parseLongId(idRequest.getId());
        if (rootCommentId == null) {
            return Result.error("一级评论ID非法");
        }
        List<ForumComment> comments = commentMapper.selectList(new LambdaQueryWrapper<ForumComment>()
                .eq(ForumComment::getRootId, rootCommentId)
                .ne(ForumComment::getId, rootCommentId)
                .orderByAsc(ForumComment::getCreateTime));
        Long currentUserId = UserHolder.getUser().getId();
        Map<Long, UserRemoteClient.UserSimpleInfo> userInfoMap = new HashMap<>();
        List<CommentLevel2VO> voList = comments.stream().map(comment -> {
            CommentLevel2VO vo = new CommentLevel2VO();
            BeanUtils.copyProperties(comment, vo);
            UserRemoteClient.UserSimpleInfo userSimpleInfo = userInfoMap.computeIfAbsent(comment.getUserId(), userRemoteClient::getUserSimpleInfo);
            vo.setUsername(userSimpleInfo.getUsername());
            vo.setAvatar(userSimpleInfo.getAvatar());
            vo.setId(String.valueOf(comment.getId()));
            vo.setUserId(String.valueOf(comment.getUserId()));
            Long replyUserId = fetchReplyUserId(comment.getReplyToCommentId());
            vo.setReplyUserId(replyUserId == null ? null : String.valueOf(replyUserId));
            vo.setLikeStatus(Boolean.TRUE.equals(stringRedisTemplate.hasKey(buildCommentLikeKey(currentUserId, comment.getId()))));
            vo.setLikeCount(comment.getLikeCount() == null ? 0 : comment.getLikeCount());
            return vo;
        }).toList();
        return Result.success(voList);
    }

    private String buildCommentLikeKey(Long userId, Long commentId) {
        return "forum:comment:like:" + userId + "_" + commentId;
    }

    private Long fetchReplyUserId(Long replyToCommentId) {
        if (replyToCommentId == null || replyToCommentId <= 0) {
            return null;
        }
        ForumComment target = commentMapper.selectById(replyToCommentId);
        return target == null ? null : target.getUserId();
    }

    private Long parseLongId(String idText) {
        try {
            return Long.valueOf(idText);
        } catch (Exception e) {
            return null;
        }
    }

    private Long parseLongIdOrDefault(String idText, Long defaultValue) {
        Long parsed = parseLongId(idText);
        return parsed == null ? defaultValue : parsed;
    }
}
