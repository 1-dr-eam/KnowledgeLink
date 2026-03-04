package com.liuyi.fateqq.controller;

import com.liuyi.fateqq.model.HistoryMessage;
import com.liuyi.fateqq.model.MessageRecord;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.model.User;
import com.liuyi.fateqq.service.MessageRecordService;

import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/talk")
public class OpenChatController {

    @Autowired
    private MessageRecordService messageRecordService;

    @Autowired
    private UserService userService;

    //处理打开窗口将所有消息标记为已读，并且推送100条消息
    @GetMapping("/open")
    public Result openChat(@RequestParam Integer friendId) {
        try {
            // 通过 request 获取 session
            HttpSession session = SessionUtil.getSession();
            if (session == null) {
                System.out.println("[OpenChatController] Session is null");
                return Result.error("用户未登录");
            }

            //从session中获取当前用户ID
            Integer currentUserId = (Integer) session.getAttribute("user_id");

            if (currentUserId == null) {
                return Result.error("用户未登录");
            }

            // 标记当前用户作为接收者的未读消息为已读（返回的是标记已读的信息的数量）
            int markedCount = messageRecordService.markMessagesAsRead(currentUserId, friendId);

//            //测试
            System.out.println("标记了"+markedCount+"条消息为已读");

            // 获取该好友的全部消息（最近100条）
            List<MessageRecord> allMessages = messageRecordService.getMessagesByUsers(
                    currentUserId, friendId,
                    100
            );

//            //测试
            System.out.println(allMessages);


            //创建消息列表
            List<HistoryMessage> messageList = new ArrayList<>();
            for (MessageRecord messageRecord : allMessages) {
                HistoryMessage historyMessage = new HistoryMessage();
                historyMessage.setId(messageRecord.getMessageId());
                historyMessage.setType(messageRecord.getIsImage()?"image":"text");
                historyMessage.setContent(messageRecord.getContent());
                historyMessage.setSender(messageRecord.getFromUserId().equals(friendId) ? "friend" : "me");
                historyMessage.setSendTime(messageRecord.getSendTime());
                messageList.add(historyMessage);
            }

            User friend = userService.selectUserById(friendId);
            Map<String,Object> responseData = new HashMap<>();
            responseData.put("id",friendId);
            responseData.put("name",friend.getUsername());
            responseData.put("avatar",friend.getAvatar());
            responseData.put("message",messageList);

            System.out.println("标记了"+markedCount+"条消息为已读");
            System.out.println("为用户 " + currentUserId + " 加载了与好友 " + friendId + " 的 " + allMessages.size() + " 条聊天记录");
            //当用户打开聊天窗口时，客户端主动发起一个 GET 请求，服务端返回该聊天窗口的初始化数据（如历史消息、未读标记等）。http协议
            return Result.success(responseData);

        } catch (Exception e) {
            System.out.println("处理打开聊天窗口异常: " + e.getMessage());
            e.printStackTrace();
            return Result.error("打开聊天窗口失败: " + e.getMessage());
        }
    }
}