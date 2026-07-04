package com.github.chat.controller;

import com.github.chat.dto.ChatMessageSendDTO;
import com.github.chat.entity.ChatMessage;
import com.github.chat.service.IChatMessageService;
import com.github.common.dto.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聊天检索控制器
 *
 * @author ning
 * @date 2026/03/24
 */
@RestController
@RequestMapping("/message")
public class ChatMessageController {
    @Autowired
    private IChatMessageService chatMessageService;

    @PostMapping("/uploadImage")
    public Result uploadImage(@RequestParam("file") MultipartFile file) {
        return Result.success(chatMessageService.uploadImage(file));
    }

    @PostMapping("/send")
    public Result sendMessage(@RequestBody ChatMessageSendDTO sendDTO) {
        ChatMessage message = chatMessageService.sendMessage(sendDTO);
        return Result.success(toResponse(message));
    }

    @GetMapping("/history")
    public Result getHistoryMessages(@RequestParam("id") String id) {
        return Result.success(chatMessageService.getHistoryMessages(id));
    }

    private Map<String, Object> toResponse(ChatMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("id", message.getId());
        payload.put("senderId", String.valueOf(message.getSenderId()));
        payload.put("receiverId", String.valueOf(message.getReceiverId()));
        payload.put("messageType", message.getMessageType());
        payload.put("message", message.getMessage());
        payload.put("sendTime", message.getSendTime());
        payload.put("read", message.getRead());
        return payload;
    }
}
