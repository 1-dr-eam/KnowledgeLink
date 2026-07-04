package com.github.forum.config;

import com.github.forum.service.ForumEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Forum es init runner
 *
 * @author ning
 * @date 2026/04/02
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ForumEsInitRunner implements ApplicationRunner {
    private final ForumEsService forumEsService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            forumEsService.initIndexAndSync();
            log.info("forum ES 索引初始化完成");
        } catch (Exception e) {
            log.error("forum ES 索引初始化失败", e);
        }
    }
}
