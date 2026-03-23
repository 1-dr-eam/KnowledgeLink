package com.github.chat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.chat.entity.ChatUser;
import com.github.chat.mapper.ChatUserMapper;
import com.github.chat.service.IChatMessageService;
import com.github.chat.vo.OpenChatVO;
import com.github.common.dto.Result;
import com.github.common.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/talk")
public class OpenChatController {
    @Autowired
    private IChatMessageService chatMessageService;
    @Autowired
    private ChatUserMapper chatUserMapper;

    @GetMapping("/open")
    public Result openChat(Long id) {
        Long userId = UserHolder.getUser().getId();
        ChatUser currentUser = chatUserMapper.selectById(userId);
        ChatUser targetUser = chatUserMapper.selectById(id);
        if (targetUser == null) {
            return Result.error("获取目标用户失败");
        }
        OpenChatVO openChatVO = new OpenChatVO();
        openChatVO.setCurrentUser(currentUser);
        openChatVO.setTargetUser(targetUser);
        openChatVO.setMessageRecords(chatMessageService.getHistoryMessage(userId, id, 100));
        chatMessageService.markRead(id, userId);
        return Result.success(openChatVO);
    }

    @GetMapping("/friends")
    public Result listFriends() {
        Long userId = UserHolder.getUser().getId();
        return Result.success(chatUserMapper.selectList(new LambdaQueryWrapper<ChatUser>().ne(ChatUser::getId, userId).last("limit 100")));
    }
}
