package com.github.chat.service;

import com.github.chat.entity.ChatUser;
import com.github.chat.vo.ChatListVO;
import com.github.chat.vo.ChatUserSimpleVO;

import java.util.List;

public interface IChatUserListService {
    List<ChatListVO> getFriendChatList();

    List<ChatListVO> getRecentChatList();

    List<ChatUserSimpleVO> searchUsersByKeyword(String keyword);

    List<ChatUserSimpleVO> getOnlineUsers();

    void markUserOnline(Long userId);

    void markUserOffline(Long userId);
}
