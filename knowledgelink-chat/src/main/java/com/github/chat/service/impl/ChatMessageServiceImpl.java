package com.github.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.chat.dto.ChatMessageSendDTO;
import com.github.chat.entity.ChatMessage;
import com.github.chat.mapper.ChatMessageMapper;
import com.github.chat.service.IChatMessageService;
import com.github.chat.vo.MessageListVO;
import com.github.common.utils.CosUtil;
import com.github.common.utils.IdCompareUtil;
import com.github.common.utils.UserHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天消息服务实现类
 *
 * @author ning
 * @date 2026/03/24
 */
@Service
public class ChatMessageServiceImpl implements IChatMessageService {
    private static final int HISTORY_LIMIT = 50;
    private final ChatMessageMapper chatMessageMapper;
    private final IdCompareUtil idCompareUtil;
    private final CosUtil cosUtil;

    public ChatMessageServiceImpl(ChatMessageMapper chatMessageMapper, IdCompareUtil idCompareUtil, CosUtil cosUtil) {
        this.chatMessageMapper = chatMessageMapper;
        this.idCompareUtil = idCompareUtil;
        this.cosUtil = cosUtil;
    }

    @Override
    public ChatMessage sendMessage(ChatMessageSendDTO sendDTO) {
        Long userId = UserHolder.getUser().getId();
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(buildMessageId(userId, sendDTO.getToUserId()));
        chatMessage.setSenderId(userId);
        chatMessage.setReceiverId(sendDTO.getToUserId());
        chatMessage.setMessage(sendDTO.getMessage());
        chatMessage.setMessageType(ChatMessage.MessageType.TEXT);
        chatMessage.setRead(Boolean.FALSE);
        chatMessage.setSendTime(LocalDateTime.now());
        saveMessage(chatMessage);
        return chatMessage;
    }

    @Override
    public void saveMessage(ChatMessage chatMessage) {
        if (chatMessage.getSendTime() == null) {
            chatMessage.setSendTime(LocalDateTime.now());
        }
        if (chatMessage.getId() == null || chatMessage.getId().isBlank()) {
            chatMessage.setId(buildMessageId(chatMessage.getSenderId(), chatMessage.getReceiverId()));
        }
        if (chatMessage.getMessageType() == null) {
            chatMessage.setMessageType(ChatMessage.MessageType.TEXT);
        }
        if (chatMessage.getRead() == null) {
            chatMessage.setRead(Boolean.FALSE);
        }
        chatMessageMapper.insert(chatMessage);
    }

    @Override
    public List<MessageListVO> getHistoryMessages(Long targetUserId) {
        Long userId = UserHolder.getUser().getId();
        String messagePrefix = idCompareUtil.idCompare(userId, targetUserId) + "_";
        List<ChatMessage> records = chatMessageMapper.selectList(new LambdaQueryWrapper<ChatMessage>()
                .likeRight(ChatMessage::getId, messagePrefix)
                .orderByDesc(ChatMessage::getSendTime)
                .last("limit " + HISTORY_LIMIT));
        List<MessageListVO> history = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            ChatMessage record = records.get(i);
            MessageListVO messageListVO = new MessageListVO();
            messageListVO.setId(record.getId());
            messageListVO.setMessage(record.getMessage());
            messageListVO.setSendTime(record.getSendTime());
            messageListVO.setType(record.getMessageType() == ChatMessage.MessageType.IMAGE ? MessageListVO.MessageType.IMAGE : MessageListVO.MessageType.TEXT);
            messageListVO.setSender(userId.equals(record.getSenderId()) ? MessageListVO.Sender.ME : MessageListVO.Sender.OPPOSITE);
            history.add(messageListVO);
        }
        return history;
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return cosUtil.uploadImage(file);
    }

    private String buildMessageId(Long senderId, Long receiverId) {
        return idCompareUtil.idCompare(senderId, receiverId) + "_" + System.currentTimeMillis();
    }
}
