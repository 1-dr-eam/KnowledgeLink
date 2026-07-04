package com.github.chat.service;

import com.github.chat.dto.ChatMessageSendDTO;
import com.github.chat.entity.ChatMessage;
import com.github.chat.vo.MessageListVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 聊天消息服务接口
 *
 * @author ning
 * @date 2026/03/24
 */
public interface IChatMessageService {
    ChatMessage sendMessage(ChatMessageSendDTO sendDTO);

    void saveMessage(ChatMessage chatMessage);

    List<MessageListVO> getHistoryMessages(String targetUserId);

    String uploadImage(MultipartFile file);
}
