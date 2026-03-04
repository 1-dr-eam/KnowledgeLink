package com.liuyi.fateqq.service;

import com.liuyi.fateqq.mapper.MessageRecordMapper;
import com.liuyi.fateqq.model.MessageRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 消息记录服务类
 * 处理消息的离线存储、查询和状态管理
 */
@Service
@Transactional
public class MessageRecordService {

    @Autowired
    private  MessageRecordMapper messageRecordMapper;

    /**
     * 保存消息到数据库
     * 所有消息都先保存到数据库，再判断是否实时发送
     */
    public int  saveMessage(MessageRecord messageRecord) {
        try {
            // 设置发送时间为当前时间
            if (messageRecord.getSendTime() == null) {
                messageRecord.setSendTime(new Date());
            }

            //调用DAO层的Mapper将数据实际插入数据库中
            return messageRecordMapper.insertMessageRecord(messageRecord);
        } catch (Exception e) {
            System.out.println("保存消息失败"+e.getMessage());
            return 0;
        }
    }



    /**
     * 根据两个用户ID查询他们之间的消息
     */
    public List<MessageRecord> getMessagesByUsers(Integer user1, Integer user2, Integer limit) {
        try {
            return messageRecordMapper.selectMessagesByUsers(user1, user2, limit);
        } catch (Exception e) {
            System.out.println("获取用户间消息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取当前用户和好友之间最后一条聊天消息
     */
    public MessageRecord getLastMessage(Integer user1, Integer user2) {
        try {
            return messageRecordMapper.selectLastMessage(user1, user2);
        } catch (Exception e) {
            System.out.println("获取最后一条消息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 根据消息ID查询消息
     */
    public MessageRecord getMessageById(String messageId) {
        try {
            return messageRecordMapper.selectMessageById(messageId);
        } catch (Exception e) {
            System.out.println("根据ID查询消息失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 获取有消息好友列表
     */
    public  List<Integer> getFriendWithUnread(Integer userId) {
        try {
            return messageRecordMapper.selectFriendsWithMessages(userId);
        } catch (Exception e) {
            System.out.println("获取有未读消息的好友列表失败: " + e.getMessage());
            return null;
        }
    }


    /**
     * 标记指定好友的所有未读消息为已读
     */
    public int markMessagesAsRead(Integer userId, Integer friendId) {
        try {
            return messageRecordMapper.markMessagesAsRead(userId, friendId);
        } catch (Exception e) {
            System.out.println("标记消息为已读失败: " + e.getMessage());
            return 0;
        }
    }

    // 在 MessageRecordService.java 中添加
    /**
     * 获取有聊天记录的好友列表（包括发送和接收）
     */
    public List<Integer> getFriendsWithChatHistory(Integer userId) {
        try {
            return messageRecordMapper.selectFriendsWithChatHistory(userId);
        } catch (Exception e) {
            System.out.println("获取有聊天记录的好友列表失败: " + e.getMessage());
            return null;
        }
    }













}