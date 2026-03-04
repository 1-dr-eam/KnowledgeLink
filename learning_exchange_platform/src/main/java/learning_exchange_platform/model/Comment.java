package learning_exchange_platform.model;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//直接回复帖子的评论
@Data
public class Comment {
    private BigInteger id;//评论本身id
    private Integer post_id;//所评论的帖子id
    private Integer user_id;//发表评论的人的id
    private String user_name;//发表评论的人的昵称
    private String user_avatar;//发表评论的人的头像
    private String content;
    private LocalDateTime create_time;
    private Integer reply_count=0;//该楼下共有多少条评论（数量变化逻辑写到插入ReplyComment中）
    private List<ReplyComment> replies=null;

    public Comment(Integer post_id, Integer user_id, String user_name, String user_avatar, String content) {
        this.post_id = post_id;
        this.user_id = user_id;
        this.user_name = user_name;
        this.user_avatar = user_avatar;
        this.content = content;
    }

    public Comment(BigInteger id, Integer post_id, Integer user_id, String user_name, String user_avatar, String content, LocalDateTime create_time, Integer reply_count) {
        this.id = id;
        this.post_id = post_id;
        this.user_id = user_id;
        this.user_name = user_name;
        this.user_avatar = user_avatar;
        this.content = content;
        this.create_time = create_time;
        this.reply_count = reply_count;
    }
}
