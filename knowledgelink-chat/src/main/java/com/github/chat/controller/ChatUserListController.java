package com.github.chat.controller;

import com.github.chat.service.IChatUserListService;
import com.github.common.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 打开聊天控制器
 *
 * @author ning
 * @date 2026/03/24
 */
@RestController
@RequestMapping("/talk")
public class ChatUserListController {
    @Autowired
    private IChatUserListService chatUserListService;

    @GetMapping("/friends")
    public Result listFriends() {
        return Result.success(chatUserListService.getFriendChatList());
    }

    @GetMapping("/recent")
    public Result listRecentChats() {
        return Result.success(chatUserListService.getRecentChatList());
    }

    @GetMapping("/users")
    public Result searchUsers(String keyword) {
        return Result.success(chatUserListService.searchUsersByKeyword(keyword));
    }

    @GetMapping("/onlineUsers")
    public Result onlineUsers() {
        return Result.success(chatUserListService.getOnlineUsers());
    }
}
