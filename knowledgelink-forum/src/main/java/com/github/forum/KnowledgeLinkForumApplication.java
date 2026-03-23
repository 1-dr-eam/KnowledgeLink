package com.github.forum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author ning
 * @date 2026/03/11
 */
@SpringBootApplication(scanBasePackages = {"com.github.forum", "com.github.common"})
@MapperScan("com.github.forum.mapper")
public class KnowledgeLinkForumApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkForumApplication.class, args);
    }
}
