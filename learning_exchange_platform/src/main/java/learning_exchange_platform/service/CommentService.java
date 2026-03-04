package learning_exchange_platform.service;

import learning_exchange_platform.mapper.CommentMapper;
import learning_exchange_platform.mapper.PostMapper;
import learning_exchange_platform.mapper.ReplyCommentMapper;
import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.Comment;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional
public class CommentService {
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;

    public boolean insertComment(Integer post_id,int user_id,String content) {
        try {
            User user=userMapper.selectUserById(user_id);
            Comment comment=new Comment(post_id,user_id,user.getUsername(),user.getAvatar(),content);
            commentMapper.insertComment(comment);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Comment> getCommentsByPostId(BigInteger post_id) {
        try {
            return commentMapper.selectCommentsByPostId(post_id);
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean deleteCommentById(BigInteger id) {
        try {
            return commentMapper.deleteCommentById(id);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

}
