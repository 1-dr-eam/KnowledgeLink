package learning_exchange_platform.model;

import lombok.Data;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Post {
    private Integer id;
    private Integer user_id;
    private String title;
    private LocalDate publish_date;
    private String summary;
    private String content;
    private String author_name;
    private String author_avatar;
    private Integer page_views;
    private String label;
    private String cover_avatar;
    private String type;
    private String visible_range;
    private Integer like_count;
    private Integer collect_count;
    private Integer comment_count;
    private String subject;//分类
    private String sub_classify;//子分类

    public Post(Integer user_id, String title, LocalDate publish_date,String summary, String content, String author_name
            , String author_avatar, String label, String cover_avatar, String type, String visible_range) {
        this.user_id = user_id;
        this.title = title;
        this.publish_date = publish_date;
        this.summary = summary;
        this.content = content;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.label = label;
        this.cover_avatar = cover_avatar;
        this.type = type;
        this.visible_range = visible_range;
    }

    public Post(Integer id, Integer user_id, String title, String summary, String content, String author_name
            , String author_avatar, String label, String cover_avatar, String type, String visible_range) {
        this.id=id;
        this.user_id = user_id;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.label = label;
        this.cover_avatar = cover_avatar;
        this.type = type;
        this.visible_range = visible_range;
    }

    public Post(Integer id, Integer user_id, String title, String summary, String content, String author_name
            , String author_avatar, String label, String cover_avatar, String type, String visible_range,String subject, String sub_classify) {
        this.id=id;
        this.user_id = user_id;
        this.title = title;
        this.summary = summary;
        this.content = content;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.label = label;
        this.cover_avatar = cover_avatar;
        this.type = type;
        this.visible_range = visible_range;
        this.subject = subject;
        this.sub_classify = sub_classify;
    }

    public Post(Integer id, Integer user_id, String title, LocalDate publish_date, String summary, String content, String author_name, String author_avatar, Integer page_views, String label, String cover_avatar, String type, String visible_range, Integer like_count, Integer collect_count, Integer comment_count, String subject, String sub_classify) {
        this.id = id;
        this.user_id = user_id;
        this.title = title;
        this.publish_date = publish_date;
        this.summary = summary;
        this.content = content;
        this.author_name = author_name;
        this.author_avatar = author_avatar;
        this.page_views = page_views;
        this.label = label;
        this.cover_avatar = cover_avatar;
        this.type = type;
        this.visible_range = visible_range;
        this.like_count = like_count;
        this.collect_count = collect_count;
        this.comment_count = comment_count;
        this.subject = subject;
        this.sub_classify = sub_classify;
    }
}
