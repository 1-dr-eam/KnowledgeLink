package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IInteractionsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 交互作用控制器
 *
 * @author ning
 * @date 2026/04/03
 */
@RestController
@RequestMapping("/interactions")
public class InteractionsController {
    @Autowired
    private IInteractionsService interactionsService;

    @PostMapping("/like")
    public Result like(String forumId) {
        return interactionsService.like(parseId(forumId));
    }

    @PostMapping("/collect")
    public Result collect(String forumId) {
        return interactionsService.collect(parseId(forumId));
    }

    @PutMapping("/unlike")
    public Result unlike(String forumId) {
        return interactionsService.unlike(parseId(forumId));
    }

    @PutMapping("/uncollect")
    public Result uncollect(String forumId) {
        return interactionsService.uncollect(parseId(forumId));
    }

    private Long parseId(String idText) {
        try {
            return Long.valueOf(idText);
        } catch (Exception e) {
            return null;
        }
    }
}
