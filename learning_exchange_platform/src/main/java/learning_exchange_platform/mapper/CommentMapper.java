package learning_exchange_platform.mapper;

import learning_exchange_platform.model.Comment;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigInteger;
import java.util.List;

@Mapper
public interface CommentMapper {
    public boolean insertComment(Comment comment);
    public boolean deleteCommentById(BigInteger id);
    public boolean deleteCommentsByIds(List<BigInteger> comment_ids);
    public Comment selectCommentById(BigInteger id);
    // 根据帖子ID查询评论列表(最上层，不含回复某评论的评论)
    List<Comment> selectCommentsByPostId(BigInteger post_id);
    // 统计帖子评论数量
    int countCommentsByPostId(BigInteger post_id);
    //当占楼评论被回复时，reply_count加1
    boolean updateReplyCount(Comment reply_comment);
}
