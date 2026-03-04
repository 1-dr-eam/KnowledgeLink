package com.liuyi.fateqq.websocket;

import com.alibaba.fastjson2.JSON;
import com.liuyi.fateqq.model.MessageRecord;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.model.User;
import learning_exchange_platform.service.UserService;
import com.liuyi.fateqq.websocket.model.Message;
import com.liuyi.fateqq.service.MessageRecordService;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import learning_exchange_platform.config.RabbitMQConfig;  // 导入配置类

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
//@RabbitListener(queues = RabbitMQConfig.QUEUE_NAME)// 添加RabbitMQ监听注解
public class ChatWebSocketHandler extends TextWebSocketHandler {

    // 维护在线用户，用户ID到WebSocketSession的映射
    @Getter
    private static final Map<Integer, WebSocketSession> onlineUsers = new ConcurrentHashMap<>();

    // 维护所有会话
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    private MessageRecordService messageRecordService;

    @Autowired
    private UserService userService;


    //连接建立成功后调用
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WebSocket连接建立: {}", session.getId());
        sessions.put(session.getId(), session);

        // 从 session 属性中获取 HttpSession
        Map<String, Object> attributes = session.getAttributes();
        HttpSession httpSession = (HttpSession) attributes.get("HTTP_SESSION");

        if (httpSession == null) {
            log.error("未找到 HttpSession，关闭连接");
            session.close();
            return;
        }

        // 从 HttpSession 中获取用户ID
        Integer currentUserId = (Integer) httpSession.getAttribute("user_id");
        log.info("获取到用户ID: {}", currentUserId);

        if (currentUserId == null) {
            log.warn("用户ID为空，关闭连接");
            session.close();
            return;
        }
        // 将用户ID保存到 WebSocketSession 属性中
        session.getAttributes().put("currentUserId", currentUserId);

        // 将用户添加到在线列表
        onlineUsers.put(currentUserId, session);

        //推送好友列表和最后一条消息
        pushFriendListWithLastMessage(session, currentUserId);
        //推送双向关注的好友
        pushFocusFriendList(session, currentUserId);

        // 推送在线好友给当前用户
        pushOnlineFriendsToCurrentUser(session, currentUserId);

        log.info("显示在线onlineUsers: {}", onlineUsers.keySet());

        // 广播自己的在线状态
        broadcastMyOnlineStatusToFriends(currentUserId, true);
    }

    //推送有聊天记录的好友列表
    private void pushFriendListWithLastMessage(WebSocketSession session, Integer currentUserId) {
        try {
            // 从聊天记录中获取有聊天记录的ID列表
            List<Integer> chatFriendIds = messageRecordService.getFriendsWithChatHistory(currentUserId);

            if (chatFriendIds == null || chatFriendIds.isEmpty()) {
                log.info("用户 {} 没有聊天记录的好友", currentUserId);
                return;
            }

            // 获取有聊天记录ID的完整信息
            List<User> chatfriends = userService.getUserByIds(chatFriendIds);
            if (chatfriends == null || chatfriends.isEmpty()) {
                log.info("未找到好友信息");
                return;
            }

            // 构建好友列表数据
            List<Map<String, Object>> chatfriendList = new ArrayList<>();
            for (User friend : chatfriends) {
                // 获取与好友的最后一条消息
                MessageRecord lastMessage = messageRecordService.getLastMessage(currentUserId, friend.getId());

                Map<String, Object> friendData = new HashMap<>();
                friendData.put("id", friend.getId());
                friendData.put("name", friend.getUsername());
                friendData.put("avatar", friend.getAvatar());

                if (lastMessage != null) {
                    friendData.put("content", lastMessage.getContent());
                    friendData.put("sendTime", lastMessage.getSendTime());
                    friendData.put("isRead", lastMessage.getIsRead());
                } else {
                    friendData.put("content", "");
                    friendData.put("sendTime", new Date());
                    friendData.put("isRead", true);
                }

                chatfriendList.add(friendData);
            }

            // 构建完整的好友列表消息
            Map<String, Object> friendListData = new HashMap<>();
            friendListData.put("type", "friend");
            friendListData.put("list", chatfriendList);

            String friendListMessage = JSON.toJSONString(Result.success(friendListData));

            // Spring WebSocket 发送消息方式
            session.sendMessage(new TextMessage(friendListMessage));

            log.info("已为用户 {} 推送 {} 个有聊天记录的好友列表", currentUserId, chatfriendList.size());

        } catch (Exception e) {
            log.error("推送有聊天记录的好友列表失败", e);
        }
    }

    //推送双向关注的好友
    private void pushFocusFriendList(WebSocketSession session, Integer currentUserId) {
        try {
            // 获取双向关注的好友列表
            List<User> focusfriends = userService.getFriends(currentUserId);
            if (focusfriends == null || focusfriends.isEmpty()) {
                return;
            }

            // 构建好友列表数据
            List<Map<String, Object>> focusfriendList = new ArrayList<>();
            for (User friend : focusfriends) {
                // 获取与好友的最后一条消息
                MessageRecord lastMessage = messageRecordService.getLastMessage(currentUserId, friend.getId());

               List<User> message = new ArrayList<>();
                Map<String, Object> friendData = new HashMap<>();
                friendData.put("id", friend.getId());
                friendData.put("name", friend.getUsername());
                friendData.put("avatar", friend.getAvatar());
                friendData.put("message", message );

                if (lastMessage != null) {
                    friendData.put("content", lastMessage.getContent());
                    friendData.put("sendTime", lastMessage.getSendTime());
                    friendData.put("isRead", lastMessage.getIsRead());
                } else {
                    friendData.put("content", "");
                    friendData.put("sendTime", new Date());
                    friendData.put("isRead", true);
                }

                focusfriendList.add(friendData);
            }

            // 构建完整的好友列表消息
            Map<String, Object> friendListData = new HashMap<>();
            friendListData.put("type", "focusfriend");
            friendListData.put("list", focusfriendList);

            String friendListMessage = JSON.toJSONString(Result.success(friendListData));

            // Spring WebSocket 发送消息方式
            session.sendMessage(new TextMessage(friendListMessage));

            log.info("已为用户 {} 推送 {} 个双向关注的好友列表", currentUserId, focusfriendList.size());

        } catch (Exception e) {
            log.error("推送双向关注好友列表失败", e);
        }
    }

    //向用户推送在线好友
    private void pushOnlineFriendsToCurrentUser(WebSocketSession session, Integer currentUserId) {
        try {
            // 获取双向关注的好友列表
            List<User> friends = userService.getFriends(currentUserId);

            if (friends == null || friends.isEmpty()) {
                return;
            }

            // 获取在线好友对应的列表
            List<Integer> onlineFriendIds = new ArrayList<>();
            for (User friend : friends) {
                if (onlineUsers.containsKey(friend.getId())) {
                    onlineFriendIds.add(friend.getId());
                }
            }

            // 构建在线状态消息
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("type", "online");
            statusData.put("list", onlineFriendIds);

            String statusMessage = JSON.toJSONString(Result.success(statusData));

            // Spring WebSocket 发送消息方式
            session.sendMessage(new TextMessage(statusMessage));

            log.info("已为用户 {} 推送 {} 个在线好友", currentUserId, onlineFriendIds.size());

        } catch (Exception e) {
            log.error("推送在线好友失败", e);
        }
    }

    //接收和发送消息时调用
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            log.info("收到消息: {}", message.getPayload());

            // 获取当前用户ID
            Integer currentUserId = (Integer) session.getAttributes().get("currentUserId");
            if (currentUserId == null) {
                log.warn("未找到用户ID，忽略消息");
                return;
            }
            // 直接解析为消息对象，不再判断类型
            Message msg = JSON.parseObject(message.getPayload(), Message.class);
            // 发送实时消息
            handleSendMessage(msg, session, currentUserId);
        } catch (Exception e) {
            log.error("处理消息异常", e);
        }
    }

    //发送实时消息
    private void handleSendMessage(Message msg, WebSocketSession session, Integer currentUserId) {
        try {
            // 保存消息到数据库
            MessageRecord savedMessage = saveMessageToDatabase(msg, currentUserId);
            if (savedMessage == null) {
                log.error("保存消息到数据库失败");
                return;
            }

            // 检查接收者是否在线
            Integer toUserId = Integer.valueOf(msg.getToUserId());
            WebSocketSession toSession = onlineUsers.get(toUserId);

            // 获取当前发送者用户信息
            User currentUser = userService.selectUserById(currentUserId);

            if (toSession != null && toSession.isOpen()) {
                // 接收者在线就将消息实时推送

                // 创建实时消息（使用统一的ResultMessage格式）
                Map<String, Object> messageData = new HashMap<>();
                messageData.put("id", savedMessage.getMessageId());
                // 判断是否是http开头（目前来说是这样）
                messageData.put("type", msg.getMessage().startsWith("http") ? "image" : "message");
                messageData.put("sender", "friend");
                messageData.put("content", msg.getMessage());
                messageData.put("sendTime", savedMessage.getSendTime());

                Map<String, Object> realtimeData = new HashMap<>();
                realtimeData.put("type", "message");
                realtimeData.put("sender_id", currentUserId);
                realtimeData.put("sender_name", currentUser.getUsername());
                realtimeData.put("sender_avatar", currentUser.getAvatar());
                realtimeData.put("message", messageData);

                // 将数据转化为json发送给前端
                String realtimeMessage = JSON.toJSONString(Result.success(realtimeData));

                // Spring WebSocket 发送消息方式
                toSession.sendMessage(new TextMessage(realtimeMessage));

                log.info("消息实时发送成功给用户 {}", toUserId);

            } else {
                // 接收者不在线将消息保存到数据库
                log.info("接收者 {} 不在线，消息已保存到数据库", toUserId);
            }

        } catch (Exception e) {
            log.error("发送消息异常", e);
        }
    }

    //向指定好友广播当前自己的在线状态(目前没用）
    private void pushOnlineStatusToPointFriend(Integer fromUserId, Integer toUserId,
                                               boolean isOnline, boolean isMutualFollow) {
        try {
            WebSocketSession toSession = onlineUsers.get(toUserId);
            if (toSession == null || !toSession.isOpen()) {
                log.info("好友 {} 不在线，无法推送状态", toUserId);
                return;
            }

            User fromUser = userService.selectUserById(fromUserId);
            if (fromUser == null) {
                log.error("用户 {} 信息不存在", fromUserId);
                return;
            }

            // 构建状态消息
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("type", "status_change");
            statusData.put("id", fromUserId);
            statusData.put("is_online", isOnline);
            statusData.put("name", fromUser.getUsername());
            statusData.put("avatar", fromUser.getAvatar());
            statusData.put("is_focus_push", true);
            statusData.put("is_mutual_follow", isMutualFollow); // 新增：互关标记
            statusData.put("timestamp", System.currentTimeMillis());

            String statusMessage = JSON.toJSONString(Result.success(statusData));

            synchronized (toSession) {
                if (toSession.isOpen()) {
                    toSession.sendMessage(new TextMessage(statusMessage));
                    log.info("互关推送: 用户{} -> 用户{}, 状态={}",
                            fromUserId, toUserId, isOnline ? "在线" : "离线");
                }
            }

        } catch (Exception e) {
            log.error("推送在线状态失败", e);
        }
    }


    //同步互关后的信息
    public void syncMutualFollowStatus(Integer user1Id, Integer user2Id) {
        try {

            log.info("互关状态同步: 用户{} 和 用户{}", user1Id, user2Id);

            // 检查双方是否在线
            boolean user1Online = onlineUsers.containsKey(user1Id);
            boolean user2Online = onlineUsers.containsKey(user2Id);

            //如果用户1在线就更新用户1好友列表和在线状态
            if (user1Online) {

                WebSocketSession Session = onlineUsers.get(user1Id);
                //推送双向关注的好友
                pushFocusFriendList(Session, user1Id);
                // 推送在线好友给当前用户
                pushOnlineFriendsToCurrentUser(Session, user1Id);

            }

            //如果用户2在线就更新用户2好友列表和在线状态
            if (user2Online) {
                WebSocketSession Session = onlineUsers.get(user2Id);
                //推送双向关注的好友
                pushFocusFriendList(Session, user2Id);
                // 推送在线好友给当前用户
                pushOnlineFriendsToCurrentUser(Session, user2Id);
            }

            log.info("互关状态同步完成: 用户1在线={}, 用户2在线={}",
                    user1Online, user2Online);

        } catch (Exception e) {
            log.error("互关状态同步失败", e);
        }
    }

//    @RabbitHandler
//    public void handleRabbitMQMessage(Map<String, Object> message) {
//        log.info("收到RabbitMQ消息: {}", message);
//
//        try {
//            String eventType = (String) message.get("eventType");
//
//            // 只处理互关事件
//            if ("USER_MUTUAL_FOLLOW".equals(eventType)) {
//                Integer user1Id = (Integer) message.get("user1Id");
//                Integer user2Id = (Integer) message.get("user2Id");
//
//                if (user1Id == null || user2Id == null) {
//                    log.error("用户ID为空: user1Id={}, user2Id={}", user1Id, user2Id);
//                    return;
//                }
//
//                log.info("从RabbitMQ处理互关事件: {} <-> {}", user1Id, user2Id);
//
//                // 调用现有的同步方法
//                this.syncMutualFollowStatus(user1Id, user2Id);
//            } else {
//                log.warn("未知的事件类型: {}", eventType);
//            }
//
//        } catch (Exception e) {
//            log.error("处理RabbitMQ消息异常", e);
//            // 抛出异常让RabbitMQ重试
//            throw new RuntimeException("处理失败: " + e.getMessage(), e);
//        }
//    }



    //向所有好友广播当前用户的在线状态(互关的时候调用这个函数)
    private void broadcastMyOnlineStatusToFriends(Integer currentUserId, boolean isOnline) {
        try {
            // 获取双向关注的好友列表
            List<User> friends = userService.getFriends(currentUserId);

            if (friends == null || friends.isEmpty()) {
                return;
            }

            // 构建在线状态变更消息
            Map<String, Object> statusData = new HashMap<>();
            statusData.put("type", "status_change");
            statusData.put("id", currentUserId);
            statusData.put("is_online", isOnline);

            // 获取当前用户信息
            User currentUser = userService.selectUserById(currentUserId);
            if (currentUser != null) {
                statusData.put("name", currentUser.getUsername());
                statusData.put("avatar", currentUser.getAvatar());
            }

            String statusMessage = JSON.toJSONString(Result.success(statusData));

            // 向所有在线好友广播状态变更
            for (User friend : friends) {
                WebSocketSession friendSession = onlineUsers.get(friend.getId());
                if (friendSession != null && friendSession.isOpen()) {
                    try {
                        friendSession.sendMessage(new TextMessage(statusMessage));
                        log.debug("向好友 {} 广播用户 {} 的{}状态",
                                friend.getId(), currentUserId, isOnline ? "上线" : "下线");
                    } catch (IOException e) {
                        log.error("向好友 {} 发送消息失败", friend.getId(), e);
                    }
                }
            }

        } catch (Exception e) {
            log.error("广播在线状态失败", e);
        }
    }

    //互关时
    //连接关闭时调用
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("WebSocket连接关闭: {}，状态: {}", session.getId(), status);

        // 移除会话
        sessions.remove(session.getId());

        // 获取用户ID并移除在线状态
        Integer currentUserId = (Integer) session.getAttributes().get("currentUserId");
        if (currentUserId != null) {
            onlineUsers.remove(currentUserId);
            // 广播离线状态
            broadcastMyOnlineStatusToFriends(currentUserId, false);
            log.info("用户 {} 已下线，当前在线用户数: {}", currentUserId, onlineUsers.size());
        }
    }

    //传输错误的时候调用
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WebSocket传输错误: {}", exception.getMessage(), exception);
    }

    //保存消息到数据库
    private MessageRecord saveMessageToDatabase(Message msg, Integer currentUserId) {
        try {
            MessageRecord messageRecord = new MessageRecord();
            messageRecord.setFromUserId(currentUserId);
            // 接收者和消息主体是通过解析前端发送过的消息解析出来的
            messageRecord.setToUserId(Integer.valueOf(msg.getToUserId()));
            messageRecord.setContent(msg.getMessage());
            // 将当前系统的发送时间设置为发送时间
            messageRecord.setSendTime(new Date());
            // 设置默认的已读未读为未读
            messageRecord.setIsRead(false);
            messageRecord.setIsImage(false);
            // 对于这里的消息由于底层insert返回的是行数
            int saveSuccess = messageRecordService.saveMessage(messageRecord);

            if (saveSuccess > 0) {
                // 返回保存的消息对象
                return messageRecord;
            }
            return null;
        } catch (Exception e) {
            log.error("保存消息到数据库异常", e);
            return null;
        }
    }


}