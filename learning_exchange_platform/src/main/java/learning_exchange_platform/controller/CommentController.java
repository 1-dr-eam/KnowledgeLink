package learning_exchange_platform.controller;

import learning_exchange_platform.model.Comment;
import learning_exchange_platform.model.ReplyComment;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.service.CommentService;
import learning_exchange_platform.service.PostService;
import learning_exchange_platform.service.ReplyCommentService;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpSession;
import java.math.BigInteger;
import java.util.List;

@RestController
public class CommentController {
    @Autowired
    private CommentService commentService;//占楼评论
    @Autowired
    private ReplyCommentService replyCommentService;//非占楼评论
    @Autowired
    private UserService userService;

    @RequestMapping("/insertComment")
    public Result insertComment(String comment_type,BigInteger comment_id,String reply_type,Integer post_id,BigInteger reply_comment_id,String content) {
        /**
         * comment_type:评论本身的类型，是占楼评论还是非占楼
         * reply_type:如果是非占楼评论，是回复占楼评论的，还是回复非占楼评论的
         * comment_id:如果是非占楼评论，属于哪个占楼评论楼下（该占楼评论的ID）
         */
        try {
            HttpSession session= SessionUtil.getSession();
            Integer user_id = (Integer)session.getAttribute("user_id");
            if(comment_type.equals("comment")) {
                commentService.insertComment(post_id,user_id,content);
            } else if (comment_type.equals("reply")) {
                replyCommentService.insertComment(comment_id,reply_type,reply_comment_id,user_id,content);
            }
            userService.postCommentsInc(post_id);
            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("评论失败");
        }
    }

    //根据帖子ID获取所有占楼评论
    @RequestMapping("/getCommentsByPostId")
    public Result getCommentsByPostId(BigInteger post_id) {
        try {
            List<Comment> comments = commentService.getCommentsByPostId(post_id);
            return Result.success(comments);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取评论列表失败");
        }
    }

    //根据占楼评论的ID获取所有回复该占楼评论的非占楼评论,传入占楼评论ID
    @RequestMapping("/getRepliesByCommentId")
    public Result getRepliesByParentId(BigInteger comment_id) {
        try {
            List<ReplyComment> replies= replyCommentService.getRepliesByCommentId(comment_id);
            return Result.success(replies);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取回复失败");
        }
    }

    @RequestMapping("/deleteCommentById")
    public Result deleteCommentById(String comment_type, BigInteger comment_id) {
        /**
         * comment_type:评论本身的类型，是占楼评论还是非占楼
         * comment_id:被删除评论的ID，传入时并不知道是占楼还是非占楼
         */
        boolean success = false;
        if(comment_type.equals("comment")) {
            success=commentService.deleteCommentById(comment_id);
        } else if (comment_type.equals("reply")) {
            success=replyCommentService.deleteReplyCommentsById(comment_id);
        }

        if (success) {
            return Result.success();
        }else {
            return Result.error("删评失败");
        }
    }

}
