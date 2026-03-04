package com.liuyi.fateqq.mapper;

import com.liuyi.fateqq.model.MessageRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

@Mapper
public interface MessageRecordMapper {

    //插入新消息
    int insertMessageRecord(MessageRecord messageRecord);
    // 根据两个用户ID查询他们之间的消息
    List<MessageRecord> selectMessagesByUsers(Integer user1, Integer user2, Integer limit);
    // 获取当前用户和好友之间最后一条消息
    MessageRecord selectLastMessage(Integer user1, Integer user2);
    //获取有消息的好友列表
    List<Integer> selectFriendsWithMessages(@Param("userId") Integer userId);
    //将好友和发送者之间的未读消息标记为已读
    int markMessagesAsRead(@Param("userId") Integer userId, @Param("friendId") Integer friendId);
    //通过当前用户ID查询对应的有聊天记录的人
    List<Integer> selectFriendsWithChatHistory(@Param("userId") Integer userId);
    //通过消息ID查询消息
    MessageRecord selectMessageById(@Param("messageId") String messageId);



}
