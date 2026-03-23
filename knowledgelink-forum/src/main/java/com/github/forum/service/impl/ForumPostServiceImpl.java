package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.common.dto.Result;
import com.github.common.dto.UserDTO;
import com.github.common.utils.UserHolder;
import com.github.forum.entity.ForumPost;
import com.github.forum.entity.ForumPostCollect;
import com.github.forum.mapper.ForumPostCollectMapper;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.service.IForumPostService;
import com.github.forum.vo.PostTitleVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class ForumPostServiceImpl implements IForumPostService {
    @Autowired
    private ForumPostMapper forumPostMapper;
    @Autowired
    private ForumPostCollectMapper forumPostCollectMapper;

    @Override
    public Result insertPost(String title, String summary, String content, String coverAvatar, String label, String type, String visibleRange, String subject, String subClassify) {
        UserDTO userDTO = UserHolder.getUser();
        ForumPost post = new ForumPost();
        post.setUserId(userDTO.getId());
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content);
        post.setCoverAvatar(coverAvatar);
        post.setLabel(label);
        post.setType(type);
        post.setVisibleRange(visibleRange == null ? "公开" : visibleRange);
        post.setSubject(subject);
        post.setSubClassify(subClassify);
        post.setAuthorName(userDTO.getUsername());
        post.setPageViews(0);
        post.setLikeCount(0);
        post.setCollectCount(0);
        post.setCommentCount(0);
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        forumPostMapper.insert(post);
        return Result.success();
    }

    @Override
    public Result deletePost(Long id) {
        forumPostMapper.deleteById(id);
        return Result.success();
    }

    @Override
    public Result updatePost(Long id, String title, String summary, String content, String coverAvatar, String label, String type, String visibleRange, String subject, String subClassify) {
        UserDTO userDTO = UserHolder.getUser();
        ForumPost post = forumPostMapper.selectById(id);
        if (post == null) {
            return Result.error("更新帖子失败");
        }
        if (!post.getUserId().equals(userDTO.getId())) {
            return Result.error("无权限更新帖子");
        }
        post.setTitle(title);
        post.setSummary(summary);
        post.setContent(content);
        post.setCoverAvatar(coverAvatar);
        post.setLabel(label);
        post.setType(type);
        post.setVisibleRange(visibleRange);
        post.setSubject(subject);
        post.setSubClassify(subClassify);
        post.setAuthorName(userDTO.getUsername());
        post.setUpdateTime(LocalDateTime.now());
        forumPostMapper.updateById(post);
        return Result.success();
    }

    @Override
    public Result getPostsByUserId() {
        Long userId = UserHolder.getUser().getId();
        List<ForumPost> postList = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId));
        return Result.success(postList);
    }

    @Override
    public Result getUserPostsById(Long userId, String collation, String searchKey) {
        List<ForumPost> postList = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId));
        return Result.success(filterAndSort(postList, collation, searchKey));
    }

    @Override
    public Result getAllPosts(String keyword, String collation, String label, String classify) {
        LambdaQueryWrapper<ForumPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ForumPost::getVisibleRange, "公开");
        if (label != null && !label.isBlank()) {
            wrapper.eq(ForumPost::getLabel, label);
        }
        if (classify != null && !classify.isBlank()) {
            wrapper.eq(ForumPost::getSubClassify, classify);
        }
        List<ForumPost> postList = forumPostMapper.selectList(wrapper);
        return Result.success(filterAndSort(postList, collation, keyword));
    }

    @Override
    public Result getLoginUserPosts(String collation) {
        Long userId = UserHolder.getUser().getId();
        List<ForumPost> posts = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getUserId, userId));
        return Result.success(sortPosts(posts, collation));
    }

    @Override
    public Result getLoginUserCollectPosts() {
        Long userId = UserHolder.getUser().getId();
        List<Long> postIds = forumPostCollectMapper.selectList(new LambdaQueryWrapper<ForumPostCollect>().eq(ForumPostCollect::getUserId, userId))
                .stream().map(ForumPostCollect::getPostId).toList();
        if (postIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        return Result.success(forumPostMapper.selectBatchIds(postIds));
    }

    @Override
    public Result getPostInfoById(Long postId) {
        ForumPost post = forumPostMapper.selectById(postId);
        return post == null ? Result.error("获取帖子信息失败") : Result.success(post);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result incPageViewsById(Long id) {
        forumPostMapper.update(null, new LambdaUpdateWrapper<ForumPost>()
                .eq(ForumPost::getId, id)
                .setSql("page_views = ifnull(page_views,0) + 1"));
        return Result.success();
    }

    @Override
    public Result getPostLabelById(Long id) {
        ForumPost post = forumPostMapper.selectById(id);
        return post == null ? Result.error("获取帖子话题失败") : Result.success(post.getLabel());
    }

    @Override
    public Result getPopularTopic() {
        List<PostTitleVO> voList = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>()
                        .eq(ForumPost::getVisibleRange, "公开"))
                .stream()
                .sorted((a, b) -> Double.compare(score(b), score(a)))
                .limit(10)
                .map(post -> {
                    PostTitleVO vo = new PostTitleVO();
                    vo.setId(post.getId());
                    vo.setTitle(post.getTitle());
                    return vo;
                }).toList();
        return Result.success(voList);
    }

    @Override
    public Result getPostLabels() {
        List<String> labels = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getVisibleRange, "公开"))
                .stream().map(ForumPost::getLabel).filter(s -> s != null && !s.isBlank()).distinct().toList();
        return Result.success(labels);
    }

    @Override
    public Result getPostClassifies() {
        List<String> classifies = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>().eq(ForumPost::getVisibleRange, "公开"))
                .stream().map(ForumPost::getSubClassify).filter(s -> s != null && !s.isBlank()).distinct().toList();
        return Result.success(classifies);
    }

    @Override
    public Result getTopicSimilarPosts(Long id, String subject, String subClassify) {
        List<PostTitleVO> list = forumPostMapper.selectList(new LambdaQueryWrapper<ForumPost>()
                        .eq(ForumPost::getVisibleRange, "公开")
                        .ne(ForumPost::getId, id)
                        .and(w -> w.eq(ForumPost::getSubject, subject).or().eq(ForumPost::getSubClassify, subClassify)))
                .stream().limit(10).map(post -> {
                    PostTitleVO vo = new PostTitleVO();
                    vo.setId(post.getId());
                    vo.setTitle(post.getTitle());
                    return vo;
                }).toList();
        return Result.success(list);
    }

    private List<ForumPost> filterAndSort(List<ForumPost> postList, String collation, String keyword) {
        String key = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        List<ForumPost> filtered = postList.stream().filter(post -> post.getTitle() != null && post.getTitle().toLowerCase(Locale.ROOT).contains(key)).toList();
        return sortPosts(filtered, collation);
    }

    private List<ForumPost> sortPosts(List<ForumPost> postList, String collation) {
        List<ForumPost> copy = new ArrayList<>(postList);
        if (collation == null || collation.isBlank() || "综合".equals(collation) || "time".equals(collation)) {
            copy.sort(Comparator.comparing(ForumPost::getCreateTime).reversed());
            return copy;
        }
        if ("最新".equals(collation)) {
            copy.sort(Comparator.comparing(ForumPost::getCreateTime).reversed());
            return copy;
        }
        if ("热门".equals(collation)) {
            copy.sort(Comparator.comparing(ForumPost::getPageViews, Comparator.nullsFirst(Integer::compareTo)).reversed());
            return copy;
        }
        copy.sort((a, b) -> Double.compare(score(b), score(a)));
        return copy;
    }

    private double score(ForumPost post) {
        return (post.getPageViews() == null ? 0 : post.getPageViews()) / 50.0 * 0.4
                + (post.getCommentCount() == null ? 0 : post.getCommentCount()) * 0.3
                + (post.getLikeCount() == null ? 0 : post.getLikeCount()) * 0.15
                + (post.getCollectCount() == null ? 0 : post.getCollectCount()) * 0.15;
    }
}
