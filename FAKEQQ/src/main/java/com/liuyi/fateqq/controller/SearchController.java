package com.liuyi.fateqq.controller;

import com.liuyi.fateqq.model.MessageRecord;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.model.User;
import com.liuyi.fateqq.service.MessageRecordService;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.SessionUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/search")
public class SearchController {

    @Autowired
    private MessageRecordService messageRecordService;

    @Autowired
    private UserService userService;

    // 搜索用户接口 - 优先从聊天记录中搜索，如果没找到再从双向关注中搜索
    @GetMapping("/users")
    public Result searchUsers(@RequestParam String keyword) {
        try {
            // 通过 request 获取 session
            HttpSession session = SessionUtil.getSession();
            if (session == null) {
                System.out.println("[SearchController] Session is null");
                return Result.error("用户未登录");
            }

            // 从 session 中获取当前用户 ID
            Integer currentUserId = (Integer) session.getAttribute("user_id");
            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            if (keyword == null || keyword.trim().isEmpty()) {
                // 返回与 ChatWebSocketHandler 推送格式一致的空列表
                Map<String, Object> friendListData = new HashMap<>();
                friendListData.put("type", "search_result");
                friendListData.put("list", Collections.emptyList());
                return Result.success(friendListData);
            }

            String searchKeyword = keyword.trim().toLowerCase();
            System.out.println("用户 " + currentUserId + " 搜索关键词: " + searchKeyword);

            List<Map<String, Object>> searchFriendList = new ArrayList<>();
            boolean foundInChatHistory = false;

            // 1. 首先从聊天记录中搜索
            List<Integer> chatFriendIds = messageRecordService.getFriendsWithChatHistory(currentUserId);

            if (chatFriendIds != null && !chatFriendIds.isEmpty()) {
                // 获取聊天好友的完整信息
                List<User> chatFriends = userService.getUserByIds(chatFriendIds);
                if (chatFriends != null && !chatFriends.isEmpty()) {
                    for (User friend : chatFriends) {
                        if (friend.getUsername().toLowerCase().contains(searchKeyword)) {
                            searchFriendList.add(buildFriendData(friend, currentUserId));
                            foundInChatHistory = true;
                        }
                    }

                    if (foundInChatHistory) {
                        System.out.println("在聊天记录中找到 " + searchFriendList.size() + " 个匹配好友");
                    }
                }
            }

            // 2. 如果在聊天记录中没找到，再从双向关注好友中搜索
            if (!foundInChatHistory) {
                List<User> focusFriends = userService.getFriends(currentUserId);
                if (focusFriends != null && !focusFriends.isEmpty()) {
                    for (User friend : focusFriends) {
                        if (friend.getUsername().toLowerCase().contains(searchKeyword)) {
                            searchFriendList.add(buildFriendData(friend, currentUserId));
                        }
                    }

                    if (!searchFriendList.isEmpty()) {
                        System.out.println("在双向关注中找到 " + searchFriendList.size() + " 个匹配好友");
                    }
                }
            }

            // 如果两个地方都没找到
            if (searchFriendList.isEmpty()) {
                System.out.println("未找到匹配的用户");
            }

            // 构建返回数据 - 格式与 ChatWebSocketHandler 推送好友列表的格式完全一致
            Map<String, Object> friendListData = new HashMap<>();
            friendListData.put("type", "search_result");
            friendListData.put("list", searchFriendList);

            System.out.println("为用户 " + currentUserId + " 返回 " + searchFriendList.size() + " 个搜索结果");

            return Result.success(friendListData);

        } catch (Exception e) {
            System.out.println("搜索用户异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("搜索失败: " + e.getMessage());
        }
    }

    // 构建好友数据 - 与 ChatWebSocketHandler 中的 buildFriendData 格式完全一致
    private Map<String, Object> buildFriendData(User friend, Integer currentUserId) {
        // 获取与好友的最后一条消息
        MessageRecord lastMessage = messageRecordService.getLastMessage(currentUserId, friend.getId());

        Map<String, Object> friendData = new HashMap<>();
        friendData.put("id", friend.getId());
        friendData.put("name", friend.getUsername());
        friendData.put("avatar", friend.getAvatar());

        if (lastMessage != null) {
            friendData.put("content", lastMessage.getContent());
            friendData.put("sendTime", lastMessage.getSendTime());
            friendData.put("isRead", lastMessage.getIsRead());
        } else {
            friendData.put("content", "");
            friendData.put("sendTime", new Date());
            friendData.put("isRead", true);
        }

        return friendData;
    }

}