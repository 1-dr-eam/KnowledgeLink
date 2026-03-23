package com.github.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.chat.entity.ChatMessageRecord;
import com.github.chat.mapper.ChatMessageRecordMapper;
import com.github.chat.service.IChatMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class ChatMessageServiceImpl implements IChatMessageService {
    @Autowired
    private ChatMessageRecordMapper chatMessageRecordMapper;

    @Override
    public List<ChatMessageRecord> getHistoryMessage(Long userId, Long targetUserId, Integer limit) {
        int size = limit == null || limit <= 0 ? 100 : limit;
        List<ChatMessageRecord> records = chatMessageRecordMapper.selectList(new LambdaQueryWrapper<ChatMessageRecord>()
                .and(wrapper -> wrapper
                        .and(w -> w.eq(ChatMessageRecord::getFromUserId, userId).eq(ChatMessageRecord::getToUserId, targetUserId))
                        .or()
                        .and(w -> w.eq(ChatMessageRecord::getFromUserId, targetUserId).eq(ChatMessageRecord::getToUserId, userId)))
                .orderByDesc(ChatMessageRecord::getSendTime)
                .last("limit " + size));
        return records.stream().sorted(Comparator.comparing(ChatMessageRecord::getSendTime)).toList();
    }

    @Override
    public void saveMessage(ChatMessageRecord messageRecord) {
        if (messageRecord.getSendTime() == null) {
            messageRecord.setSendTime(LocalDateTime.now());
        }
        chatMessageRecordMapper.insert(messageRecord);
    }

    @Override
    public void markRead(Long fromUserId, Long toUserId) {
        chatMessageRecordMapper.update(null, new LambdaUpdateWrapper<ChatMessageRecord>()
                .eq(ChatMessageRecord::getFromUserId, fromUserId)
                .eq(ChatMessageRecord::getToUserId, toUserId)
                .eq(ChatMessageRecord::getRead, false)
                .set(ChatMessageRecord::getRead, true));
    }
}
