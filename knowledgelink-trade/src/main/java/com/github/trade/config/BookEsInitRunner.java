package com.github.trade.config;

import com.github.trade.service.BookEsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 书籍索引初始化索引构建
 *
 * @author ning
 * @date 2026/03/16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookEsInitRunner implements ApplicationRunner {
    private final BookEsService bookEsService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            bookEsService.initIndexAndSync();
            log.info("book ES 索引初始化完成");
        } catch (Exception e) {
            log.error("book ES 索引初始化失败", e);
        }
    }
}
