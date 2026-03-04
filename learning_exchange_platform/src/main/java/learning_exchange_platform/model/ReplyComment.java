package learning_exchange_platform.model;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDateTime;

//回复评论的评论(非占楼评论)
@Data
public class ReplyComment {
    private BigInteger id;//评论自己的id
    private BigInteger comment_id;//属于哪个占楼评论的ID
    private String reply_type;//回复的评论类型：comment-占楼评论, reply-回复评论
    private BigInteger reply_comment_id;//回复某评论的ID，该评论可能为占楼或非占楼
    private String reply_user_name;//所回复的评论作者昵称
    private String content;//内容
    private Integer user_id;//发表评论的人的id
    private String user_name;//发表评论的人的昵称
    private String user_avatar;//发表评论的人的头像
    private LocalDateTime create_time;

    public ReplyComment(BigInteger comment_id, String reply_type, BigInteger reply_comment_id, String reply_user_name, String content, Integer user_id, String user_name, String user_avatar) {
        this.comment_id = comment_id;
        this.reply_type = reply_type;
        this.reply_comment_id = reply_comment_id;
        this.reply_user_name = reply_user_name;
        this.content = content;
        this.user_id = user_id;
        this.user_name = user_name;
        this.user_avatar = user_avatar;
    }

    public ReplyComment(BigInteger id, BigInteger comment_id, String reply_type, BigInteger reply_comment_id, String reply_user_name, String content, Integer user_id, String user_name, String user_avatar, LocalDateTime create_time) {
        this.id = id;
        this.comment_id = comment_id;
        this.reply_type = reply_type;
        this.reply_comment_id = reply_comment_id;
        this.reply_user_name = reply_user_name;
        this.content = content;
        this.user_id = user_id;
        this.user_name = user_name;
        this.user_avatar = user_avatar;
        this.create_time = create_time;
    }
}
