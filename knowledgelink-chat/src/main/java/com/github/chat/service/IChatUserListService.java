package com.github.chat.service;

import com.github.chat.entity.ChatUser;
import com.github.chat.vo.ChatListVO;

import java.util.List;

public interface IChatUserListService {
    List<ChatListVO> getFriendChatList();

    List<ChatListVO> getRecentChatList();

    List<ChatUser> searchUsersByKeyword(String keyword);

    List<ChatUser> getOnlineUsers();

    void markUserOnline(Long userId);

    void markUserOffline(Long userId);
}
