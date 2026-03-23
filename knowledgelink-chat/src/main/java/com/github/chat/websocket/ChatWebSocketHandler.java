package com.github.chat.websocket;

import cn.hutool.json.JSONUtil;
import com.github.chat.dto.ChatSendMessageDTO;
import com.github.chat.entity.ChatMessageRecord;
import com.github.chat.service.IChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final Map<Long, WebSocketSession> ONLINE_USER_MAP = new ConcurrentHashMap<>();

    @Autowired
    private IChatMessageService chatMessageService;

    public static Map<Long, WebSocketSession> getOnlineUserMap() {
        return ONLINE_USER_MAP;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        ONLINE_USER_MAP.put(Long.valueOf(String.valueOf(userId)), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId == null) {
            return;
        }
        ONLINE_USER_MAP.remove(Long.valueOf(String.valueOf(userId)));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Object userIdObj = session.getAttributes().get("userId");
        if (userIdObj == null) {
            return;
        }
        Long fromUserId = Long.valueOf(String.valueOf(userIdObj));
        ChatSendMessageDTO sendMessageDTO = JSONUtil.toBean(message.getPayload(), ChatSendMessageDTO.class);
        if (sendMessageDTO.getToUserId() == null || sendMessageDTO.getMessage() == null || sendMessageDTO.getMessage().isBlank()) {
            return;
        }
        ChatMessageRecord record = new ChatMessageRecord();
        record.setMessageId(UUID.randomUUID().toString().replace("-", ""));
        record.setFromUserId(fromUserId);
        record.setToUserId(sendMessageDTO.getToUserId());
        record.setContent(sendMessageDTO.getMessage());
        record.setSendTime(LocalDateTime.now());
        record.setRead(false);
        record.setImage("image".equalsIgnoreCase(sendMessageDTO.getType()));
        chatMessageService.saveMessage(record);

        String response = JSONUtil.toJsonStr(record);
        session.sendMessage(new TextMessage(response));
        WebSocketSession targetSession = ONLINE_USER_MAP.get(sendMessageDTO.getToUserId());
        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(response));
        }
    }
}
