package com.liuyi.fateqq.controller;

import com.alibaba.fastjson2.JSON;
import com.liuyi.fateqq.model.MessageRecord;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.utils.OSSUtil;
import learning_exchange_platform.model.User;
import com.liuyi.fateqq.service.MessageRecordService;
import learning_exchange_platform.service.UserService;
import com.liuyi.fateqq.websocket.ChatWebSocketHandler;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileUploadController {

    @Autowired
    private OSSUtil ossUtil;

    @Autowired
    private MessageRecordService messageRecordService;
    @Autowired
    private UserService userService;

    //定义图片上传接口
    @RequestMapping(value = "/uploadImageMessage")
    public Result uploadImageMessage(@RequestParam("file") MultipartFile file,
                                     @RequestParam("toUserId") Integer toUserId,
                                     HttpSession session) {
        try {
            // 验证用户登录
            Integer currentUserId = (Integer) session.getAttribute("user_id");

            // 验证文件
            if (file.isEmpty()) {
                return Result.error("文件不能为空");
            }

            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return Result.error("只能上传图片文件");
            }

            // 验证文件大小 (限制5MB)
            if (file.getSize() > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }

            // 上传图片到OSS转化成url格式存储
            String imageUrl = ossUtil.OSSUpload(file);
            if (imageUrl == null) {
                return Result.error("图片上传失败");
            }

            // 保存消息到数据库
            MessageRecord messageRecord = saveImageMessageToDatabase(currentUserId, toUserId, imageUrl);

            if (messageRecord == null) {
                return Result.error("消息保存失败");
            }

            // 实时推送给接收者
            boolean pushSuccess = pushImageMessageToReceiver(currentUserId, toUserId, messageRecord);

//            // 返回成功结果
//            Map<String, Object> data = new HashMap<>();
//            data.put("messageId", messageRecord.getMessageId());
//            data.put("imageUrl", imageUrl);
//            data.put("sendTime", messageRecord.getSendTime());

            //向发送方返回图片信息，提供前端加载方式
            return Result.success();

        } catch (Exception e) {
            System.out.println("图片上传异常: " + e.getMessage());
            return Result.error("服务器错误: " + e.getMessage());
        }
    }

    //保存图片消息
    private MessageRecord saveImageMessageToDatabase(Integer fromUserId, Integer toUserId, String imageUrl) {
        try {
            MessageRecord messageRecord = new MessageRecord();
            messageRecord.setFromUserId(fromUserId);
            messageRecord.setToUserId(toUserId);
            messageRecord.setContent(imageUrl); // 添加前缀标识
            messageRecord.setSendTime(new Date());
            messageRecord.setIsRead(false);
            messageRecord.setIsImage(true);

            int saveSuccess = messageRecordService.saveMessage(messageRecord);

            if (saveSuccess > 0) {
                System.out.println("图片消息保存成功，URL: " + imageUrl);
                return messageRecord;
            }
            return null;

        } catch (Exception e) {
            System.out.println("保存图片消息到数据库失败: " + e.getMessage());
            return null;
        }
    }

    //通过Websocekt将图片消息推送给接收者
    private boolean pushImageMessageToReceiver(Integer currentUserId, Integer toUserId, MessageRecord savedMessage) {
        try {
            // 获取接收者对应的 WebSocketSession
            Map<Integer, WebSocketSession> onlineUsers = ChatWebSocketHandler.getOnlineUsers();
            WebSocketSession toSession = onlineUsers.get(toUserId);

            // 获取当前用户信息
            User currentUser = userService.selectUserById(currentUserId);

            // 如果接收者在线就将详细推送给前端
            if (toSession != null && toSession.isOpen()) {
                // 创建实时消息（使用统一的ResultMessage格式）
                Map<String, Object> messageData = new ConcurrentHashMap<>();
                messageData.put("id", savedMessage.getMessageId());
                messageData.put("type", savedMessage.getContent().startsWith("http") ? "image" : "message");
                messageData.put("sender", "friend");
                messageData.put("content", savedMessage.getContent());
                messageData.put("sendTime", savedMessage.getSendTime());

                Map<String, Object> realtimeData = new ConcurrentHashMap<>();
                realtimeData.put("type", "message");
                realtimeData.put("sender_id", currentUserId);
                // 如果需要发送用户信息，取消注释
                // realtimeData.put("sender_name", currentUser.getUsername());
                // realtimeData.put("sender_avatar", currentUser.getAvatar());
                realtimeData.put("message", messageData);

                // 将数据转化为json发送给前端
                String realtimeMessage = JSON.toJSONString(Result.success(realtimeData));

                // Spring WebSocket 发送消息方式
                toSession.sendMessage(new TextMessage(realtimeMessage));

                log.info("图片消息实时推送成功给用户 {}", toUserId);
                return true;
            } else {
                log.info("接收者 {} 不在线，图片消息已保存到数据库", toUserId);
                return false;
            }
        } catch (Exception e) {
            log.error("WebSocket推送图片消息失败", e);
            return false;
        }
    }
}
