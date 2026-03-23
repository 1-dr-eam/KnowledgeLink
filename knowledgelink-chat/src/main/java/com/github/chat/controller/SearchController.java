package com.github.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.chat.entity.ChatUser;
import com.github.chat.mapper.ChatUserMapper;
import com.github.chat.websocket.ChatWebSocketHandler;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.WebSocketSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/search")
public class SearchController {
    @Autowired
    private ChatUserMapper chatUserMapper;

    @GetMapping("/users")
    public Result searchUsers(String keyword) {
        Long userId = UserHolder.getUser().getId();
        String key = keyword == null ? "" : keyword.trim();
        LambdaQueryWrapper<ChatUser> wrapper = new LambdaQueryWrapper<ChatUser>()
                .ne(ChatUser::getId, userId)
                .and(w -> w.like(ChatUser::getUsername, key).or().like(ChatUser::getPhone, key))
                .last("limit 20");
        List<ChatUser> users = chatUserMapper.selectList(wrapper);
        return Result.success(users);
    }

    @GetMapping("/onlineUsers")
    public Result onlineUsers() {
        Map<Long, WebSocketSession> onlineMap = ChatWebSocketHandler.getOnlineUserMap();
        if (onlineMap.isEmpty()) {
            return Result.success(new ArrayList<>());
        }
        List<ChatUser> list = chatUserMapper.selectBatchIds(onlineMap.keySet());
        return Result.success(list);
    }
}
