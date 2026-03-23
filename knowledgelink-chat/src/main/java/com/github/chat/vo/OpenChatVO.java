package com.github.chat.vo;

import com.github.chat.entity.ChatMessageRecord;
import com.github.chat.entity.ChatUser;
import lombok.Data;

import java.util.List;

@Data
public class OpenChatVO {
    private ChatUser currentUser;
    private ChatUser targetUser;
    private List<ChatMessageRecord> messageRecords;
}
