package com.github.chat.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.chat.entity.ChatMessage;
import com.github.chat.entity.ChatUser;
import com.github.chat.feign.UserFeignClient;
import com.github.chat.mapper.ChatMessageMapper;
import com.github.chat.mapper.ChatUserMapper;
import com.github.chat.service.IChatUserListService;
import com.github.chat.vo.ChatListVO;
import com.github.common.dto.Result;
import com.github.common.dto.UserDTO;
import com.github.common.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天用户列表服务实现类
 *
 * @author ning
 * @date 2026/03/29
 */
@Service
public class ChatUserListServiceImpl implements IChatUserListService {
    private static final int CHAT_LIST_LIMIT = 100;
    private static final int CHAT_LIST_SCAN_LIMIT = 1000;
    private static final String CHAT_ONLINE_USER_KEY = "chat:online:users";
    private static final String CHAT_ONLINE_PUSH_CHANNEL = "chat:online:push";
    private final UserFeignClient userFeignClient;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatUserMapper chatUserMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public ChatUserListServiceImpl(UserFeignClient userFeignClient, ChatMessageMapper chatMessageMapper, ChatUserMapper chatUserMapper, StringRedisTemplate stringRedisTemplate) {
        this.userFeignClient = userFeignClient;
        this.chatMessageMapper = chatMessageMapper;
        this.chatUserMapper = chatUserMapper;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public List<ChatListVO> getFriendChatList() {
        Long userId = UserHolder.getUser().getId();
        List<Long> friendIds = fetchFriendIds();
        if (friendIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserDTO> users = fetchUsersByIds(friendIds);
        return buildChatListByUsers(userId, users, Collections.emptySet());
    }

    @Override
    public List<ChatListVO> getRecentChatList() {
        Long userId = UserHolder.getUser().getId();
        List<Long> recentPeerIds = queryRecentPeerIds(userId);
        if (recentPeerIds == null || recentPeerIds.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> friendIdSet = new HashSet<>(fetchFriendIds());
        List<Long> targetIds = recentPeerIds.stream().filter(peerId -> !friendIdSet.contains(peerId)).toList();
        if (targetIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<UserDTO> users = fetchUsersByIds(targetIds);
        return buildChatListByUsers(userId, users, friendIdSet);
    }

    @Override
    public List<ChatUser> searchUsersByKeyword(String keyword) {
        Long userId = UserHolder.getUser().getId();
        String key = keyword == null ? "" : keyword.trim();
        return chatUserMapper.selectList(new LambdaQueryWrapper<ChatUser>()
                .ne(ChatUser::getId, userId)
                .like(ChatUser::getUsername, key)
                .last("limit 20"));
    }

    @Override
    public List<ChatUser> getOnlineUsers() {
        Set<String> onlineUserIds = stringRedisTemplate.opsForSet().members(CHAT_ONLINE_USER_KEY);
        if (onlineUserIds == null || onlineUserIds.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> ids = onlineUserIds.stream().map(Long::valueOf).toList();
        return chatUserMapper.selectBatchIds(ids);
    }

    @Override
    public void markUserOnline(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().add(CHAT_ONLINE_USER_KEY, String.valueOf(userId));
        stringRedisTemplate.convertAndSend(CHAT_ONLINE_PUSH_CHANNEL, userId + ":online");
    }

    @Override
    public void markUserOffline(Long userId) {
        if (userId == null) {
            return;
        }
        stringRedisTemplate.opsForSet().remove(CHAT_ONLINE_USER_KEY, String.valueOf(userId));
        stringRedisTemplate.convertAndSend(CHAT_ONLINE_PUSH_CHANNEL, userId + ":offline");
    }

    private List<Long> fetchFriendIds() {
        Result result = userFeignClient.getFriendIds();
        if (result == null || result.getCode() == null || result.getCode() != 1 || result.getData() == null) {
            return new ArrayList<>();
        }
        return JSONUtil.parseArray(result.getData()).toList(Long.class);
    }

    private List<UserDTO> fetchUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Result result = userFeignClient.getUsersByIds(ids);
        if (result == null || result.getCode() == null || result.getCode() != 1 || result.getData() == null) {
            return new ArrayList<>();
        }
        return JSONUtil.parseArray(result.getData()).toList(UserDTO.class);
    }

    private List<ChatListVO> buildChatListByUsers(Long userId, List<UserDTO> users, Set<Long> excludeIds) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, UserDTO> userMap = new LinkedHashMap<>();
        for (UserDTO user : users) {
            if (user == null || user.getId() == null || excludeIds.contains(user.getId())) {
                continue;
            }
            userMap.put(user.getId(), user);
        }
        Set<String> onlineUserIds = stringRedisTemplate.opsForSet().members(CHAT_ONLINE_USER_KEY);
        Set<Long> onlineIdSet = onlineUserIds == null ? new HashSet<>() : onlineUserIds.stream().map(Long::valueOf).collect(Collectors.toSet());
        List<ChatListVO> chatList = new ArrayList<>();
        for (Map.Entry<Long, UserDTO> entry : userMap.entrySet()) {
            Long peerId = entry.getKey();
            UserDTO user = entry.getValue();
            ChatMessage latestMessage = queryLatestMessageBetween(userId, peerId);
            ChatListVO chatListVO = new ChatListVO();
            chatListVO.setId(user.getId());
            chatListVO.setAvatar(user.getAvatar());
            chatListVO.setName(user.getUsername());
            chatListVO.setStatus(onlineIdSet.contains(user.getId()));
            if (latestMessage != null) {
                chatListVO.setLastMessage(latestMessage.getMessage());
                chatListVO.setRead(Boolean.TRUE.equals(latestMessage.getRead()));
                chatListVO.setSendTime(latestMessage.getSendTime());
            } else {
                chatListVO.setRead(Boolean.TRUE);
            }
            chatList.add(chatListVO);
        }
        chatList.sort((left, right) -> {
            if (left.getSendTime() == null && right.getSendTime() == null) {
                return 0;
            }
            if (left.getSendTime() == null) {
                return 1;
            }
            if (right.getSendTime() == null) {
                return -1;
            }
            return right.getSendTime().compareTo(left.getSendTime());
        });
        return chatList;
    }

    private List<Long> queryRecentPeerIds(Long userId) {
        List<ChatMessage> records = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(wrapper -> wrapper
                        .eq(ChatMessage::getSenderId, userId)
                        .or()
                        .eq(ChatMessage::getReceiverId, userId))
                .orderByDesc(ChatMessage::getSendTime)
                .last("limit " + CHAT_LIST_SCAN_LIMIT));
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> peerIds = new LinkedHashSet<>();
        for (ChatMessage record : records) {
            if (record == null) {
                continue;
            }
            Long peerId = userId.equals(record.getSenderId()) ? record.getReceiverId() : record.getSenderId();
            if (peerId == null) {
                continue;
            }
            peerIds.add(peerId);
            if (peerIds.size() >= CHAT_LIST_LIMIT) {
                break;
            }
        }
        return new ArrayList<>(peerIds);
    }

    private ChatMessage queryLatestMessageBetween(Long userId, Long peerId) {
        List<ChatMessage> records = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .and(wrapper -> wrapper
                        .and(w -> w.eq(ChatMessage::getSenderId, userId).eq(ChatMessage::getReceiverId, peerId))
                        .or()
                        .and(w -> w.eq(ChatMessage::getSenderId, peerId).eq(ChatMessage::getReceiverId, userId)))
                .orderByDesc(ChatMessage::getSendTime)
                .last("limit 1"));
        if (records == null || records.isEmpty()) {
            return null;
        }
        return records.getFirst();
    }
}
