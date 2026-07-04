package com.github.forum.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import com.github.forum.dto.ForumUpsertDTO;
import com.github.forum.dto.IdRequest;
import com.github.forum.dto.SearchDTO;
import com.github.forum.cache.model.ForumDataCacheModel;
import com.github.forum.cache.model.ForumInteractionsCacheModel;
import com.github.forum.entity.Forum;
import com.github.forum.entity.UserForumInteractions;
import com.github.forum.feign.UserFeignClient;
import com.github.forum.feign.dto.UserProfileDTO;
import com.github.forum.mapper.ForumPostMapper;
import com.github.forum.mapper.InteractionsMapper;
import com.github.forum.service.ForumEsService;
import com.github.forum.service.IForumPostService;
import com.github.forum.service.recommend.UserBehaviorProfileProvider;
import com.github.forum.vo.ExcellentCreatorVO;
import com.github.forum.vo.ForumBrowseVO;
import com.github.forum.vo.ForumDetailVO;
import com.github.forum.vo.HotForumVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子服务实现类
 * 提供帖子新增、修改、删除、按ID查询、按用户查询、详情查询、推荐框架与热点框架能力。
 * 当前版本使用 MyBatis-Plus 完成基础读写，推荐和热点业务逻辑预留后续实现。
 *
 * @author ning
 * @date 2026/04/01
 */
@Service
public class ForumPostServiceImpl implements IForumPostService {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int RECOMMEND_MIN_CANDIDATE_SIZE = 50;
    private static final int RECOMMEND_ES_FETCH_SIZE = 120;
    private static final int RECOMMEND_CACHE_FETCH_SIZE = 120;
    private static final int RECOMMEND_DB_FETCH_SIZE = 200;
    private static final int HOT_POST_SCAN_LIMIT = 500;
    private static final int CREATOR_SCAN_LIMIT = 800;
    private static final long CACHE_TTL_HOURS = 12L;
    private static final String CACHE_RECOMMEND_CANDIDATE_PREFIX = "forum:post:recommend:candidate:";
    private static final String CACHE_HOT_PREFIX = "forum:post:hot:";
    private static final String CACHE_HOT_TOPIC_PREFIX = "forum:post:hot-topic:";
    private static final String CACHE_CREATOR_PREFIX = "forum:post:excellent-creator:";
    private static final String CACHE_DETAIL_PREFIX = "forum:post:detail:user:";
    private static final String FORUM_DATA_KEY_PREFIX = "forum:data:";
    private static final String FORUM_INTERACTION_KEY_PREFIX = "forum:interaction:";
    private static final String FORUM_DATA_DIRTY_SET = "forum:data:dirty";

    @Autowired
    private ForumPostMapper forumPostMapper;
    @Autowired
    private ForumEsService forumEsService;
    @Autowired
    private InteractionsMapper interactionsMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserBehaviorProfileProvider userBehaviorProfileProvider;

    /**
     * 新增帖子
     *
     * @param forumUpsertDTO 帖子新增参数
     * @return 新增结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addPost(ForumUpsertDTO forumUpsertDTO) {
        if (forumUpsertDTO == null || forumUpsertDTO.getTitle() == null || forumUpsertDTO.getTitle().isEmpty()) {
            return Result.error("帖子标题不能为空");
        }
        Forum forum = BeanUtil.copyProperties(forumUpsertDTO, Forum.class);
        forum.setUserId(UserHolder.getUser().getId());
        forumPostMapper.insert(forum);
        try {
            forumEsService.saveForum(forum);
        } catch (Exception e) {
            throw new RuntimeException("帖子写入 ES 失败", e);
        }
        return Result.success();
    }

    /**
     * 修改帖子
     *
     * @param idRequest 帖子ID参数
     * @param forumUpsertDTO 帖子更新参数
     * @return 修改结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result updatePost(IdRequest idRequest, ForumUpsertDTO forumUpsertDTO) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank() || forumUpsertDTO == null) {
            return Result.error("帖子参数不能为空");
        }
        if (forumUpsertDTO.getTitle() == null || forumUpsertDTO.getTitle().isEmpty()) {
            return Result.error("帖子标题不能为空");
        }
        Long forumId = parseLongId(idRequest.getId());
        if (forumId == null) {
            return Result.error("帖子ID非法");
        }
        Forum forum = forumPostMapper.selectById(forumId);
        if (forum == null) {
            return Result.error("更新帖子失败");
        }
        if (!forum.getUserId().equals(UserHolder.getUser().getId())) {
            return Result.error("无权限更新帖子");
        }
        forum.setTitle(forumUpsertDTO.getTitle());
        forum.setSummary(forumUpsertDTO.getSummary());
        forum.setContent(forumUpsertDTO.getContent());
        forum.setCoverAvatar(forumUpsertDTO.getCoverAvatar());
        forum.setLabel(forumUpsertDTO.getLabel());
        forum.setSubject(forumUpsertDTO.getSubject());
        forum.setSubClassify(forumUpsertDTO.getSubClassify());
        forum.setType(forumUpsertDTO.getType());
        forum.setVisibleRange(forumUpsertDTO.getVisibleRange());
        forumPostMapper.updateById(forum);
        try {
            forumEsService.saveForum(forum);
        } catch (Exception e) {
            throw new RuntimeException("帖子更新 ES 失败", e);
        }
        return Result.success();
    }

    /**
     * 删除帖子
     *
     * @param idRequest 帖子ID参数
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result deletePost(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank()) {
            return Result.error("帖子ID不能为空");
        }
        Long forumId = parseLongId(idRequest.getId());
        if (forumId == null) {
            return Result.error("帖子ID非法");
        }
        forumPostMapper.deleteById(forumId);
        try {
            forumEsService.deleteForum(forumId);
        } catch (Exception e) {
            throw new RuntimeException("帖子删除 ES 失败", e);
        }
        return Result.success();
    }

    /**
     * 根据帖子ID查询帖子简要信息
     *
     * @param idRequest 帖子ID参数
     * @return 帖子简要信息
     */
    @Override
    public Result getPostById(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank()) {
            return Result.error("帖子ID不能为空");
        }
        Long forumId = parseLongId(idRequest.getId());
        if (forumId == null) {
            return Result.error("帖子ID非法");
        }
        Forum forum = forumPostMapper.selectById(forumId);
        if (forum == null) {
            return Result.error("帖子不存在");
        }
        return Result.success(toBrowseVO(forum));
    }

    /**
     * 根据用户ID查询该用户发布的帖子
     *
     * @param userId 用户ID
     * @return 帖子简要信息列表
     */
    @Override
    public Result getPostsByUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return Result.error("用户ID不能为空");
        }
        Long parsedUserId = parseLongId(userId);
        if (parsedUserId == null) {
            return Result.error("用户ID非法");
        }
        List<Forum> forumList = forumPostMapper.selectList(new LambdaQueryWrapper<Forum>()
                .eq(Forum::getUserId, parsedUserId)
                .orderByDesc(Forum::getCreateTime));
        return Result.success(forumList.stream().map(this::toBrowseVO).collect(Collectors.toList()));
    }

    @Override
    public Result getMyCollectedPosts() {
        Long userId = UserHolder.getUser().getId();
        List<UserForumInteractions> interactions = interactionsMapper.selectList(new LambdaQueryWrapper<UserForumInteractions>()
                .eq(UserForumInteractions::getUserId, userId)
                .eq(UserForumInteractions::getCollectStatus, true)
                .orderByDesc(UserForumInteractions::getUpdateTime)
                .last("limit 200"));
        if (interactions == null || interactions.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<Long> forumIds = interactions.stream()
                .map(UserForumInteractions::getForumId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (forumIds.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        Map<Long, Forum> forumMap = forumPostMapper.selectBatchIds(forumIds).stream()
                .collect(Collectors.toMap(Forum::getId, forum -> forum, (left, right) -> left));
        List<ForumBrowseVO> result = new ArrayList<>();
        for (Long forumId : forumIds) {
            Forum forum = forumMap.get(forumId);
            if (forum == null) {
                continue;
            }
            result.add(toBrowseVO(forum));
        }
        return Result.success(result);
    }

    /**
     * 根据帖子ID获取帖子详情
     *
     * @param idRequest 帖子ID参数
     * @return 帖子详情
     */
    @Override
    public Result getPostDetailById(IdRequest idRequest) {
        if (idRequest == null || idRequest.getId() == null || idRequest.getId().isBlank()) {
            return Result.error("帖子ID不能为空");
        }
        Long forumId = parseLongId(idRequest.getId());
        if (forumId == null) {
            return Result.error("帖子ID非法");
        }
        Long userId = UserHolder.getUser().getId();
        String detailCacheKey = buildPostDetailCacheKey(userId, forumId);
        Forum forum = forumPostMapper.selectById(forumId);
        if (forum == null) {
            return Result.error("帖子不存在");
        }
        ForumDetailVO detailVO = readCacheObject(detailCacheKey, ForumDetailVO.class);
        if (detailVO == null) {
            detailVO = BeanUtil.copyProperties(forum, ForumDetailVO.class);
            detailVO.setId(String.valueOf(forum.getId()));
            detailVO.setUserId(String.valueOf(forum.getUserId()));
            UserProfileDTO userProfile = getUserProfileById(forum.getUserId());
            detailVO.setUserName(userProfile.getUsername());
            detailVO.setUserAvatar(userProfile.getAvatar());
            writeCache(detailCacheKey, detailVO);
        } else {
            // 同步帖子基础字段，避免帖子更新后长时间命中旧详情缓存
            detailVO.setTitle(forum.getTitle());
            detailVO.setSummary(forum.getSummary());
            detailVO.setContent(forum.getContent());
            detailVO.setCoverAvatar(forum.getCoverAvatar());
            detailVO.setLabel(forum.getLabel());
            detailVO.setSubject(forum.getSubject());
            detailVO.setSubClassify(forum.getSubClassify());
            detailVO.setType(forum.getType());
            detailVO.setVisibleRange(forum.getVisibleRange());
            detailVO.setCreateTime(forum.getCreateTime());
        }
        ForumDataCacheModel forumDataCacheModel = getOrInitForumDataCacheModel(forum);
        forumDataCacheModel.setPageViews(defaultCount(forumDataCacheModel.getPageViews()) + 1);
        writeForumDataCacheModel(forumDataCacheModel);
        stringRedisTemplate.opsForSet().add(FORUM_DATA_DIRTY_SET, String.valueOf(forum.getId()));
        detailVO.setPageViews(defaultCount(forumDataCacheModel.getPageViews()));
        detailVO.setLikeCount(defaultCount(forumDataCacheModel.getLikeCount()));
        detailVO.setCollectCount(defaultCount(forumDataCacheModel.getCollectCount()));
        detailVO.setCommentCount(defaultCount(forum.getCommentCount()));
        boolean selfForum = userId != null && userId.equals(forum.getUserId());
        detailVO.setSelfForum(selfForum);
        detailVO.setFollowStatus(selfForum ? Boolean.FALSE : isFollowTargetUser(forum.getUserId()));
        UserForumInteractions interactions = getUserForumInteractions(userId, forum.getId());
        detailVO.setLikeStatus(interactions != null && Boolean.TRUE.equals(interactions.getLikeStatus()));
        detailVO.setCollectStatus(interactions != null && Boolean.TRUE.equals(interactions.getCollectStatus()));
        return Result.success(detailVO);
    }

    /**
     * 根据搜索条件检索帖子
     *
     * @param searchDTO 搜索参数
     * @return 帖子列表
     */
    @Override
    public Result searchPosts(SearchDTO searchDTO) {
        try {
            return Result.success(forumEsService.searchForums(searchDTO));
        } catch (Exception e) {
            return Result.error("搜索帖子失败");
        }
    }

    /**
     * 获取推荐帖子列表
     *
     * @param limit 返回条数限制
     * @return 推荐帖子列表
     */
    @Override
    public Result getRecommendedPosts(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        Long userId = UserHolder.getUser().getId();
        String candidateCacheKey = CACHE_RECOMMEND_CANDIDATE_PREFIX + (userId == null ? "guest" : userId);
        LinkedHashMap<String, ForumBrowseVO> candidateMap = new LinkedHashMap<>();

        List<ForumBrowseVO> esCandidates = queryEsCandidatesByUserPreference(userId, RECOMMEND_ES_FETCH_SIZE);
        appendDeduplicate(candidateMap, esCandidates);

        List<ForumBrowseVO> cacheCandidates = readCacheList(candidateCacheKey, ForumBrowseVO.class);
        if (cacheCandidates != null && !cacheCandidates.isEmpty()) {
            appendDeduplicate(candidateMap, cacheCandidates.stream().limit(RECOMMEND_CACHE_FETCH_SIZE).toList());
        }

        if (candidateMap.size() < RECOMMEND_MIN_CANDIDATE_SIZE) {
            List<ForumBrowseVO> dbCandidates = queryRecentPublicForums(RECOMMEND_DB_FETCH_SIZE).stream()
                    .map(this::toBrowseVO)
                    .toList();
            appendDeduplicate(candidateMap, dbCandidates);
            writeCache(candidateCacheKey, new ArrayList<>(candidateMap.values()));
        }

        List<ForumBrowseVO> sampled = new ArrayList<>(candidateMap.values());
        Collections.shuffle(sampled);
        List<ForumBrowseVO> result = sampled.stream()
                .limit(safeLimit)
                .sorted((left, right) -> {
                    if (left.getCreateTime() == null && right.getCreateTime() == null) {
                        return 0;
                    }
                    if (left.getCreateTime() == null) {
                        return 1;
                    }
                    if (right.getCreateTime() == null) {
                        return -1;
                    }
                    return right.getCreateTime().compareTo(left.getCreateTime());
                })
                .toList();
        return Result.success(result);
    }

    /**
     * 获取热点帖子列表
     *
     * @param limit 返回条数限制
     * @return 热点帖子列表
     */
    @Override
    public Result getHotPosts(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        String cacheKey = CACHE_HOT_PREFIX + safeLimit;
        List<ForumBrowseVO> cached = readCacheList(cacheKey, ForumBrowseVO.class);
        if (cached != null) {
            return Result.success(cached);
        }
        List<ForumBrowseVO> result = queryRecentPublicForums(HOT_POST_SCAN_LIMIT).stream()
                .sorted((left, right) -> Long.compare(calculateHotScore(right), calculateHotScore(left)))
                .limit(safeLimit)
                .map(this::toBrowseVO)
                .toList();
        writeCache(cacheKey, result);
        return Result.success(result);
    }

    @Override
    public Result getHotTopics(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        String cacheKey = CACHE_HOT_TOPIC_PREFIX + safeLimit;
        List<HotForumVO> cached = readCacheList(cacheKey, HotForumVO.class);
        if (cached != null) {
            return Result.success(cached);
        }
        List<HotForumVO> result = queryRecentPublicForums(HOT_POST_SCAN_LIMIT).stream()
                .sorted((left, right) -> Long.compare(calculateHotScore(right), calculateHotScore(left)))
                .limit(safeLimit)
                .map(forum -> {
                    HotForumVO vo = new HotForumVO();
                    vo.setId(String.valueOf(forum.getId()));
                    vo.setTitle(forum.getTitle());
                    vo.setHot(calculateHotScore(forum));
                    return vo;
                })
                .toList();
        writeCache(cacheKey, result);
        return Result.success(result);
    }

    @Override
    public Result getExcellentCreators(Integer limit) {
        int safeLimit = normalizeLimit(limit);
        String cacheKey = CACHE_CREATOR_PREFIX + safeLimit;
        List<ExcellentCreatorVO> cached = readCacheList(cacheKey, ExcellentCreatorVO.class);
        if (cached != null) {
            return Result.success(cached);
        }
        Map<Long, CreatorStat> statMap = new HashMap<>();
        for (Forum forum : queryRecentPublicForums(CREATOR_SCAN_LIMIT)) {
            CreatorStat stat = statMap.computeIfAbsent(forum.getUserId(), ignored -> new CreatorStat());
            stat.postCount++;
            stat.likeCount += defaultCount(forum.getLikeCount());
            stat.collectCount += defaultCount(forum.getCollectCount());
            stat.commentCount += defaultCount(forum.getCommentCount());
            stat.viewCount += defaultCount(forum.getPageViews());
            if (forum.getCreateTime() != null && forum.getCreateTime().isAfter(LocalDateTime.now().minusDays(30))) {
                stat.recentPostCount++;
            }
            if (forum.getLabel() != null && !forum.getLabel().isBlank()) {
                stat.labelCount.merge(forum.getLabel(), 1, Integer::sum);
            }
        }

        List<ExcellentCreatorVO> result = statMap.entrySet().stream()
                .sorted(Comparator.comparingLong((Map.Entry<Long, CreatorStat> entry) -> calculateCreatorScore(entry.getValue())).reversed())
                .limit(safeLimit)
                .map(entry -> buildExcellentCreatorVO(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .toList();
        writeCache(cacheKey, result);
        return Result.success(result);
    }

    /**
     * 将帖子实体转换为帖子浏览VO
     *
     * @param forum 帖子实体
     * @return 帖子浏览VO
     */
    private ForumBrowseVO toBrowseVO(Forum forum) {
        ForumBrowseVO browseVO = new ForumBrowseVO();
        browseVO.setId(String.valueOf(forum.getId()));
        browseVO.setUserId(String.valueOf(forum.getUserId()));
        browseVO.setTitle(forum.getTitle());
        browseVO.setSummary(forum.getSummary());
        browseVO.setCoverAvatar(forum.getCoverAvatar());
        browseVO.setLabel(forum.getLabel());
        browseVO.setSubject(forum.getSubject());
        browseVO.setSubClassify(forum.getSubClassify());
        browseVO.setType(forum.getType());
        browseVO.setVisibleRange(forum.getVisibleRange());
        browseVO.setCreateTime(forum.getCreateTime());
        UserProfileDTO userProfile = getUserProfileById(forum.getUserId());
        browseVO.setUserName(userProfile.getUsername());
        browseVO.setUserAvatar(userProfile.getAvatar());
        browseVO.setPageViews(defaultCount(forum.getPageViews()));
        browseVO.setLikeCount(defaultCount(forum.getLikeCount()));
        browseVO.setCommentCount(defaultCount(forum.getCommentCount()));
        browseVO.setCollectCount(defaultCount(forum.getCollectCount()));
        return browseVO;
    }

    private Integer defaultCount(Integer value) {
        return value == null ? 0 : value;
    }

    private UserForumInteractions getUserForumInteractions(Long userId, Long forumId) {
        if (userId == null || forumId == null) {
            return null;
        }
        ForumInteractionsCacheModel interactionsCache = readCacheObject(
                FORUM_INTERACTION_KEY_PREFIX + userId + "_" + forumId,
                ForumInteractionsCacheModel.class
        );
        if (interactionsCache != null) {
            UserForumInteractions interaction = new UserForumInteractions();
            interaction.setInteractionId(interactionsCache.getInteractionId());
            interaction.setUserId(interactionsCache.getUserId());
            interaction.setForumId(interactionsCache.getForumId());
            interaction.setLikeStatus(Boolean.TRUE.equals(interactionsCache.getLikeStatus()));
            interaction.setCollectStatus(Boolean.TRUE.equals(interactionsCache.getCollectStatus()));
            return interaction;
        }
        return interactionsMapper.selectById(userId + "_" + forumId);
    }

    private List<ForumBrowseVO> queryEsCandidatesByUserPreference(Long userId, int expectedSize) {
        UserBehaviorProfileProvider.UserInterestProfile profile = userBehaviorProfileProvider.getUserInterestProfile(userId);
        List<String> tokens = new ArrayList<>();
        if (profile.getCategories() != null) {
            tokens.addAll(normalizePreferenceTokens(profile.getCategories()));
        }
        if (profile.getKeywords() != null) {
            tokens.addAll(normalizePreferenceTokens(profile.getKeywords()));
        }
        if (tokens.isEmpty()) {
            return new ArrayList<>();
        }
        String keyword = tokens.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .distinct()
                .limit(8)
                .collect(Collectors.joining(" "));
        if (keyword.isBlank()) {
            return new ArrayList<>();
        }
        SearchDTO searchDTO = new SearchDTO();
        searchDTO.setKeyword(keyword);
        searchDTO.setSort(0);
        List<ForumBrowseVO> result;
        try {
            result = forumEsService.searchForums(searchDTO);
        } catch (Exception e) {
            return new ArrayList<>();
        }
        if (result == null || result.isEmpty()) {
            return new ArrayList<>();
        }
        return result.stream().limit(expectedSize).toList();
    }

    private List<String> normalizePreferenceTokens(List<String> source) {
        List<String> normalized = new ArrayList<>();
        if (source == null || source.isEmpty()) {
            return normalized;
        }
        for (String raw : source) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] splitTokens = raw.split("[,，;；\\s]+");
            for (String token : splitTokens) {
                if (token == null) {
                    continue;
                }
                String value = token.trim();
                if (!value.isBlank()) {
                    normalized.add(value);
                }
            }
        }
        return normalized;
    }

    private void appendDeduplicate(LinkedHashMap<String, ForumBrowseVO> candidateMap, List<ForumBrowseVO> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (ForumBrowseVO item : source) {
            if (item == null || item.getId() == null || item.getId().isBlank()) {
                continue;
            }
            candidateMap.putIfAbsent(item.getId(), item);
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private Long parseLongId(String idText) {
        try {
            return Long.valueOf(idText);
        } catch (Exception e) {
            return null;
        }
    }

    private String buildPostDetailCacheKey(Long userId, Long forumId) {
        String userPart = userId == null ? "guest" : String.valueOf(userId);
        return CACHE_DETAIL_PREFIX + userPart + ":" + forumId;
    }

    private String buildForumDataKey(Long forumId) {
        return FORUM_DATA_KEY_PREFIX + forumId;
    }

    private ForumDataCacheModel getOrInitForumDataCacheModel(Forum forum) {
        ForumDataCacheModel cacheModel = readCacheObject(buildForumDataKey(forum.getId()), ForumDataCacheModel.class);
        if (cacheModel != null) {
            return cacheModel;
        }
        cacheModel = new ForumDataCacheModel();
        cacheModel.setForumId(forum.getId());
        cacheModel.setPageViews(defaultCount(forum.getPageViews()));
        cacheModel.setLikeCount(defaultCount(forum.getLikeCount()));
        cacheModel.setCollectCount(defaultCount(forum.getCollectCount()));
        cacheModel.setCommentCount(defaultCount(forum.getCommentCount()));
        return cacheModel;
    }

    private void writeForumDataCacheModel(ForumDataCacheModel forumDataCacheModel) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildForumDataKey(forumDataCacheModel.getForumId()),
                    objectMapper.writeValueAsString(forumDataCacheModel),
                    60L,
                    TimeUnit.MINUTES
            );
        } catch (Exception ignored) {
        }
    }

    private List<Forum> queryRecentPublicForums(int scanLimit) {
        return forumPostMapper.selectList(new LambdaQueryWrapper<Forum>()
                .eq(Forum::getVisibleRange, "公开")
                .orderByDesc(Forum::getCreateTime)
                .last("limit " + scanLimit));
    }

    private long calculateHotScore(Forum forum) {
        long views = defaultCount(forum.getPageViews());
        long likes = defaultCount(forum.getLikeCount());
        long collects = defaultCount(forum.getCollectCount());
        long comments = defaultCount(forum.getCommentCount());
        long freshness = 0;
        if (forum.getCreateTime() != null) {
            long hours = Math.max(1, Duration.between(forum.getCreateTime(), LocalDateTime.now()).toHours());
            freshness = Math.max(0, 72 - hours);
        }
        return views + likes * 4 + collects * 5 + comments * 6 + freshness;
    }

    private long calculateCreatorScore(CreatorStat stat) {
        return stat.postCount * 12L
                + stat.likeCount * 3L
                + stat.collectCount * 5L
                + stat.commentCount * 4L
                + stat.viewCount / 20L
                + stat.recentPostCount * 8L;
    }

    private ExcellentCreatorVO buildExcellentCreatorVO(Long userId, CreatorStat stat) {
        UserProfileDTO userProfile = getUserProfileById(userId);
        ExcellentCreatorVO vo = new ExcellentCreatorVO();
        vo.setId(String.valueOf(userId));
        vo.setAvatar(userProfile.getAvatar());
        vo.setName(userProfile.getUsername());
        vo.setScore(calculateCreatorScore(stat));
        String dominantLabel = stat.labelCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("综合");
        vo.setIntro("擅长 " + dominantLabel + " 方向内容创作");
        return vo;
    }

    private UserProfileDTO getUserProfileById(Long userId) {
        UserProfileDTO emptyProfile = new UserProfileDTO();
        if (userId == null) {
            return emptyProfile;
        }
        try {
            Result result = userFeignClient.getUserInfoById(userId);
            if (result == null || result.getCode() == null || result.getCode() != 1 || result.getData() == null) {
                return emptyProfile;
            }
            return objectMapper.convertValue(result.getData(), UserProfileDTO.class);
        } catch (Exception e) {
            return emptyProfile;
        }
    }

    private Boolean isFollowTargetUser(Long targetUserId) {
        if (targetUserId == null) {
            return Boolean.FALSE;
        }
        try {
            Result result = userFeignClient.isFollow(targetUserId);
            if (result == null || result.getCode() == null || result.getCode() != 1 || result.getData() == null) {
                return Boolean.FALSE;
            }
            return objectMapper.convertValue(result.getData(), Boolean.class);
        } catch (Exception e) {
            return Boolean.FALSE;
        }
    }

    private <T> List<T> readCacheList(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            JavaType javaType = objectMapper.getTypeFactory().constructCollectionType(List.class, clazz);
            return objectMapper.readValue(json, javaType);
        } catch (Exception ignored) {
            return null;
        }
    }

    private <T> T readCacheObject(String key, Class<T> clazz) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, clazz);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void writeCache(String key, Object value) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }

    private static class CreatorStat {
        private int postCount;
        private int recentPostCount;
        private int likeCount;
        private int collectCount;
        private int commentCount;
        private int viewCount;
        private final Map<String, Integer> labelCount = new HashMap<>();
    }
}
