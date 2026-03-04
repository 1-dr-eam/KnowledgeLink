package learning_exchange_platform.service;

import learning_exchange_platform.mapper.CommentMapper;
import learning_exchange_platform.mapper.ReplyCommentMapper;
import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.Comment;
import learning_exchange_platform.model.ReplyComment;
import learning_exchange_platform.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigInteger;
import java.util.*;

@Service
@Transactional
public class ReplyCommentService {
    @Autowired
    private ReplyCommentMapper replyCommentMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;

    public List<ReplyComment> getRepliesByCommentId(BigInteger comment_id) {
        try {
            return replyCommentMapper.selectRepliesByCommentId(comment_id);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean insertComment(BigInteger comment_id, String reply_type, BigInteger reply_comment_id, Integer user_id, String content) {
        try {
            if(reply_type.equals("comment")) {
                Comment reply_comment=commentMapper.selectCommentById(reply_comment_id);
                reply_comment.setReply_count(reply_comment.getReply_count()+1);
                //更新回复数
                commentMapper.updateReplyCount(reply_comment);
                User user=userMapper.selectUserById(user_id);
                ReplyComment replyComment=new ReplyComment(comment_id,reply_type,reply_comment.getId(),reply_comment.getUser_name()
                        ,content,user_id,user.getUsername(),user.getAvatar());
                replyCommentMapper.insertReplyComment(replyComment);
            }else if(reply_type.equals("reply")) {
                ReplyComment reply_comment=replyCommentMapper.selectReplyCommentById(reply_comment_id);
                User user=userMapper.selectUserById(user_id);
                ReplyComment replyComment=new ReplyComment(comment_id,reply_type,reply_comment.getId(),reply_comment.getUser_name()
                        ,content,user_id,user.getUsername(),user.getAvatar());
                replyCommentMapper.insertReplyComment(replyComment);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    //使用队列进行广度优先遍历，找出所有相关评论，再一次性删除
    //传入的ID是要删除的非占楼评论ID
    public boolean deleteReplyCommentsById(BigInteger id) {
        try {
            Set<BigInteger> allReplyCommentIdsToDelete = new HashSet<>();
            allReplyCommentIdsToDelete.add(id);
            //初始化队列
            Queue<BigInteger> queue = new LinkedList<>();
            queue.offer(id);

            while (!queue.isEmpty()) {
                //取出队头ID
                BigInteger current_id = queue.poll();
                // 查询当前评论的所有直接回复的ID
                List<BigInteger> childIds = replyCommentMapper.selectRepliesIdByCurrentId(current_id);
                //BFS
                for (BigInteger childId : childIds) {
                    if (!allReplyCommentIdsToDelete.contains(childId)) {
                        allReplyCommentIdsToDelete.add(childId);
                        queue.offer(childId);
                    }
                }
            }
            // 一次性删除所有相关的评论
            if (!allReplyCommentIdsToDelete.isEmpty()) {
                replyCommentMapper.deleteReplyCommentsByIds(new ArrayList<>(allReplyCommentIdsToDelete));
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
