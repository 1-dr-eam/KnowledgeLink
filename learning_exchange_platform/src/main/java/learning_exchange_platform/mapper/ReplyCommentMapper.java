package learning_exchange_platform.mapper;

import learning_exchange_platform.model.ReplyComment;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface ReplyCommentMapper{

    boolean insertReplyComment(ReplyComment reply_comment);

    ReplyComment selectReplyCommentById(BigInteger id);

    List<ReplyComment> selectRepliesByCommentId(BigInteger comment_id);

    List<BigInteger> selectRepliesIdByCurrentId(BigInteger current_id);

    boolean deleteReplyCommentsByIds(ArrayList<BigInteger> ids);
}
