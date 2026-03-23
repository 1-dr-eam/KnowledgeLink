package com.github.chat.service;

import com.github.chat.entity.ChatMessageRecord;

import java.util.List;

public interface IChatMessageService {
    List<ChatMessageRecord> getHistoryMessage(Long userId, Long targetUserId, Integer limit);
    void saveMessage(ChatMessageRecord messageRecord);
    void markRead(Long fromUserId, Long toUserId);
}
