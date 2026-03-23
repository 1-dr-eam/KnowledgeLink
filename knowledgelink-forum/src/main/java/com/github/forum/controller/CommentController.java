package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IForumCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommentController {
    @Autowired
    private IForumCommentService forumCommentService;

    @RequestMapping("/insertComment")
    public Result insertComment(String comment_type, Long comment_id, String reply_type, Long post_id, Long reply_comment_id, String content) {
        return forumCommentService.insertComment(comment_type, comment_id, reply_type, post_id, reply_comment_id, content);
    }

    @RequestMapping("/getCommentsByPostId")
    public Result getCommentsByPostId(Long post_id) {
        return forumCommentService.getCommentsByPostId(post_id);
    }

    @RequestMapping("/getRepliesByCommentId")
    public Result getRepliesByCommentId(Long comment_id) {
        return forumCommentService.getRepliesByCommentId(comment_id);
    }

    @RequestMapping("/deleteCommentById")
    public Result deleteCommentById(String comment_type, Long comment_id) {
        return forumCommentService.deleteCommentById(comment_type, comment_id);
    }
}
