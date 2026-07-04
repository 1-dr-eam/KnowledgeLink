package com.github.forum.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.forum.cache.model.ForumDataCacheModel;
import com.github.forum.cache.model.ForumInteractionsCacheModel;
import com.github.forum.entity.Forum;
import com.github.forum.entity.UserForumInteractions;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.mapper.InteractionsMapper;
import com.github.forum.service.IInteractionsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 交互服务实施
 *
 * @author ning
 * @date 2026/04/08
 */
@Service
public class InteractionsServiceImpl implements IInteractionsService {
    private static final String FORUM_INTERACTION_KEY_PREFIX = "forum:interaction:";
    private static final String FORUM_DATA_KEY_PREFIX = "forum:data:";
    private static final String FORUM_INTERACTION_DIRTY_SET = "forum:interaction:dirty";
    private static final String FORUM_DATA_DIRTY_SET = "forum:data:dirty";
    private static final long CACHE_TTL_MINUTES = 60L;
    private static final int FLUSH_BATCH_SIZE = 200;

    private final InteractionsMapper interactionsMapper;
    private final ForumPostMapper forumPostMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public InteractionsServiceImpl(InteractionsMapper interactionsMapper, ForumPostMapper forumPostMapper, StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.interactionsMapper = interactionsMapper;
        this.forumPostMapper = forumPostMapper;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Result like(Long forumId) {
        return updateForumInteraction(forumId, true, null);
    }

    @Override
    public Result collect(Long forumId) {
        return updateForumInteraction(forumId, null, true);
    }

    @Override
    public Result unlike(Long forumId) {
        return updateForumInteraction(forumId, false, null);
    }

    @Override
    public Result uncollect(Long forumId) {
        return updateForumInteraction(forumId, null, false);
    }

    @Scheduled(fixedDelay = 5000)
    public void flushForumInteractionCache() {
        Set<String> dirtyIds = stringRedisTemplate.opsForSet().members(FORUM_INTERACTION_DIRTY_SET);
        if (dirtyIds == null || dirtyIds.isEmpty()) {
            return;
        }
        List<String> ids = limitSet(dirtyIds, FLUSH_BATCH_SIZE);
        for (String interactionId : ids) {
            ForumInteractionsCacheModel cacheModel = readCache(buildForumInteractionKey(interactionId), ForumInteractionsCacheModel.class);
            if (cacheModel == null) {
                stringRedisTemplate.opsForSet().remove(FORUM_INTERACTION_DIRTY_SET, interactionId);
                continue;
            }
            UserForumInteractions interaction = interactionsMapper.selectById(interactionId);
            if (interaction == null) {
                interaction = new UserForumInteractions();
                interaction.setInteractionId(cacheModel.getInteractionId());
                interaction.setUserId(cacheModel.getUserId());
                interaction.setForumId(cacheModel.getForumId());
                interaction.setLikeStatus(Boolean.TRUE.equals(cacheModel.getLikeStatus()));
                interaction.setCollectStatus(Boolean.TRUE.equals(cacheModel.getCollectStatus()));
                interactionsMapper.insert(interaction);
            } else {
                interaction.setLikeStatus(Boolean.TRUE.equals(cacheModel.getLikeStatus()));
                interaction.setCollectStatus(Boolean.TRUE.equals(cacheModel.getCollectStatus()));
                interactionsMapper.updateById(interaction);
            }
            stringRedisTemplate.opsForSet().remove(FORUM_INTERACTION_DIRTY_SET, interactionId);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void flushForumDataCache() {
        Set<String> forumIds = stringRedisTemplate.opsForSet().members(FORUM_DATA_DIRTY_SET);
        if (forumIds == null || forumIds.isEmpty()) {
            return;
        }
        List<String> ids = limitSet(forumIds, FLUSH_BATCH_SIZE);
        for (String forumIdText : ids) {
            Long forumId = parseLong(forumIdText);
            if (forumId == null) {
                stringRedisTemplate.opsForSet().remove(FORUM_DATA_DIRTY_SET, forumIdText);
                continue;
            }
            ForumDataCacheModel dataCacheModel = readCache(buildForumDataKey(forumId), ForumDataCacheModel.class);
            if (dataCacheModel == null) {
                stringRedisTemplate.opsForSet().remove(FORUM_DATA_DIRTY_SET, forumIdText);
                continue;
            }
            Forum forum = forumPostMapper.selectById(forumId);
            if (forum == null) {
                stringRedisTemplate.opsForSet().remove(FORUM_DATA_DIRTY_SET, forumIdText);
                continue;
            }
            forum.setPageViews(dataCacheModel.getPageViews());
            forum.setLikeCount(dataCacheModel.getLikeCount());
            forum.setCollectCount(dataCacheModel.getCollectCount());
            forum.setCommentCount(dataCacheModel.getCommentCount());
            forumPostMapper.updateById(forum);
            stringRedisTemplate.opsForSet().remove(FORUM_DATA_DIRTY_SET, forumIdText);
        }
    }

    private Result updateForumInteraction(Long forumId, Boolean likeStatus, Boolean collectStatus) {
        if (forumId == null) {
            return Result.error("帖子id不能为空");
        }
        Long userId = UserHolder.getUser().getId();
        Forum forum = forumPostMapper.selectById(forumId);
        if (forum == null) {
            return Result.error("帖子不存在");
        }
        String interactionId = buildInteractionId(userId, forumId);
        ForumInteractionsCacheModel interactionCache = getOrInitInteractionCache(userId, forumId, interactionId);
        ForumDataCacheModel forumDataCache = getOrInitForumDataCache(forum);
        boolean changed = false;
        if (likeStatus != null && !likeStatus.equals(interactionCache.getLikeStatus())) {
            interactionCache.setLikeStatus(likeStatus);
            forumDataCache.setLikeCount(calculateNextCount(forumDataCache.getLikeCount(), likeStatus ? 1 : -1));
            changed = true;
        }
        if (collectStatus != null && !collectStatus.equals(interactionCache.getCollectStatus())) {
            interactionCache.setCollectStatus(collectStatus);
            forumDataCache.setCollectCount(calculateNextCount(forumDataCache.getCollectCount(), collectStatus ? 1 : -1));
            changed = true;
        }
        if (!changed) {
            return Result.success();
        }
        writeCache(buildForumInteractionKey(interactionId), interactionCache);
        writeCache(buildForumDataKey(forumId), forumDataCache);
        stringRedisTemplate.opsForSet().add(FORUM_INTERACTION_DIRTY_SET, interactionId);
        stringRedisTemplate.opsForSet().add(FORUM_DATA_DIRTY_SET, String.valueOf(forumId));
        return Result.success();
    }

    private ForumInteractionsCacheModel getOrInitInteractionCache(Long userId, Long forumId, String interactionId) {
        String cacheKey = buildForumInteractionKey(interactionId);
        ForumInteractionsCacheModel cacheModel = readCache(cacheKey, ForumInteractionsCacheModel.class);
        if (cacheModel != null) {
            if (cacheModel.getLikeStatus() == null) {
                cacheModel.setLikeStatus(Boolean.FALSE);
            }
            if (cacheModel.getCollectStatus() == null) {
                cacheModel.setCollectStatus(Boolean.FALSE);
            }
            return cacheModel;
        }
        UserForumInteractions interaction = interactionsMapper.selectOne(new LambdaQueryWrapper<UserForumInteractions>()
                .eq(UserForumInteractions::getInteractionId, interactionId));
        cacheModel = new ForumInteractionsCacheModel();
        cacheModel.setInteractionId(interactionId);
        cacheModel.setUserId(userId);
        cacheModel.setForumId(forumId);
        cacheModel.setLikeStatus(interaction != null && Boolean.TRUE.equals(interaction.getLikeStatus()));
        cacheModel.setCollectStatus(interaction != null && Boolean.TRUE.equals(interaction.getCollectStatus()));
        writeCache(cacheKey, cacheModel);
        return cacheModel;
    }

    private ForumDataCacheModel getOrInitForumDataCache(Forum forum) {
        String cacheKey = buildForumDataKey(forum.getId());
        ForumDataCacheModel cacheModel = readCache(cacheKey, ForumDataCacheModel.class);
        if (cacheModel != null) {
            return cacheModel;
        }
        cacheModel = new ForumDataCacheModel();
        cacheModel.setForumId(forum.getId());
        cacheModel.setPageViews(safeCount(forum.getPageViews()));
        cacheModel.setLikeCount(safeCount(forum.getLikeCount()));
        cacheModel.setCollectCount(safeCount(forum.getCollectCount()));
        cacheModel.setCommentCount(safeCount(forum.getCommentCount()));
        writeCache(cacheKey, cacheModel);
        return cacheModel;
    }

    private int safeCount(Integer value) {
        return value == null ? 0 : value;
    }

    private int calculateNextCount(Integer current, int delta) {
        int next = safeCount(current) + delta;
        return Math.max(next, 0);
    }

    private String buildInteractionId(Long userId, Long forumId) {
        return userId + "_" + forumId;
    }

    private String buildForumInteractionKey(String interactionId) {
        return FORUM_INTERACTION_KEY_PREFIX + interactionId;
    }

    private String buildForumDataKey(Long forumId) {
        return FORUM_DATA_KEY_PREFIX + forumId;
    }

    private <T> T readCache(String key, Class<T> clazz) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    private void writeCache(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception ignored) {
        }
    }

    private List<String> limitSet(Set<String> values, int size) {
        List<String> result = new ArrayList<>(size);
        for (String value : values) {
            if (value == null) {
                continue;
            }
            result.add(value);
            if (result.size() >= size) {
                break;
            }
        }
        return result;
    }

    private Long parseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
    }
}
