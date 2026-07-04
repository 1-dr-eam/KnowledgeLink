package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.dto.ForumCommentDTO;
import com.github.forum.dto.IdRequest;
import com.github.forum.service.ICommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 *
 * @author ning
 * @date 2026/03/24
 */

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private ICommentService commentService;

    @PostMapping("/add")
    public Result addComment(@RequestBody ForumCommentDTO forumCommentDTO) {
        return commentService.addComment(forumCommentDTO);
    }

    @DeleteMapping("/delete")
    public Result deleteCommentById(@RequestBody IdRequest idRequest) {
        return commentService.deleteCommentById(idRequest);
    }

    @PostMapping("/level1")
    public Result getLevel1CommentsByForumId(@RequestBody IdRequest idRequest) {
        return commentService.getLevel1CommentsByForumId(idRequest);
    }

    @PostMapping("/level2")
    public Result getLevel2CommentsByRootCommentId(@RequestBody IdRequest idRequest) {
        return commentService.getLevel2CommentsByRootCommentId(idRequest);
    }
}
