package com.github.chat.vo;

import com.github.chat.entity.ChatUser;
import lombok.Data;

import java.util.List;

/**
 * 打开聊天窗口返回数据 VO
 *
 * @author ning
 * @date 2026/03/24
 */
@Data
public class OpenChatVO {
    private ChatUser currentUser;
    private ChatUser targetUser;
    private List<MessageListVO> messageRecords;
}
