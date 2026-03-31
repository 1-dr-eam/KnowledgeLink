package com.github.chat.websocket;

import cn.hutool.json.JSONUtil;
import com.github.chat.dto.ChatMessageSendDTO;
import com.github.chat.entity.ChatMessage;
import com.github.chat.service.IChatMessageService;
import com.github.chat.service.IChatUserListService;
import com.github.common.dto.UserDTO;
import com.github.common.utils.IdCompareUtil;
import com.github.common.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 消息处理器
 *
 * @author ning
 * @date 2026/03/24
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final Map<Long, WebSocketSession> ONLINE_USER_MAP = new ConcurrentHashMap<>();

    @Autowired
    private IChatMessageService chatMessageService;
    @Autowired
    private IChatUserListService chatUserListService;
    @Autowired
    private IdCompareUtil idCompareUtil;

    /**
     * 获取在线用户会话映射
     *
     * @return 在线用户会话映射
     */
    public static Map<Long, WebSocketSession> getOnlineUserMap() {
        return ONLINE_USER_MAP;
    }

    /**
     * 建立连接后将当前用户加入在线会话映射
     *
     * @param session WebSocket 会话
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            return;
        }
        ONLINE_USER_MAP.put(userId, session);
        chatUserListService.markUserOnline(userId);
    }

    /**
     * 连接关闭后移除当前用户在线状态
     *
     * @param session WebSocket 会话
     * @param status 关闭状态
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            return;
        }
        ONLINE_USER_MAP.remove(userId);
        chatUserListService.markUserOffline(userId);
    }

    /**
     * 处理文本消息并完成消息持久化及实时转发
     *
     * @param session 发送方会话
     * @param message 文本消息
     * @throws Exception 消息处理异常
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long fromUserId = resolveUserId(session);
        if (fromUserId == null) {
            return;
        }
        ChatMessageSendDTO sendMessageDTO = JSONUtil.toBean(message.getPayload(), ChatMessageSendDTO.class);
        if (sendMessageDTO.getToUserId() == null || sendMessageDTO.getMessage() == null || sendMessageDTO.getMessage().isBlank()) {
            return;
        }
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(idCompareUtil.idCompare(fromUserId, sendMessageDTO.getToUserId()) + "_" + System.currentTimeMillis());
        chatMessage.setSenderId(fromUserId);
        chatMessage.setReceiverId(sendMessageDTO.getToUserId());
        chatMessage.setMessage(sendMessageDTO.getMessage());
        chatMessage.setSendTime(LocalDateTime.now());
        chatMessage.setRead(Boolean.FALSE);
        chatMessage.setMessageType(ChatMessage.MessageType.TEXT);
        chatMessageService.saveMessage(chatMessage);

        String response = JSONUtil.toJsonStr(chatMessage);
        session.sendMessage(new TextMessage(response));
        WebSocketSession targetSession = ONLINE_USER_MAP.get(sendMessageDTO.getToUserId());
        if (targetSession != null && targetSession.isOpen()) {
            targetSession.sendMessage(new TextMessage(response));
        }
    }

    private Long resolveUserId(WebSocketSession session) {
        Object userIdObj = session.getAttributes().get(USER_ID_ATTRIBUTE);
        if (userIdObj != null) {
            return Long.valueOf(String.valueOf(userIdObj));
        }
        UserDTO userDTO = UserHolder.getUser();
        if (userDTO == null || userDTO.getId() == null) {
            return null;
        }
        return userDTO.getId();
    }
}
