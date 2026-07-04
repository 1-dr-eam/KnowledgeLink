package com.github.forum;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 论坛服务启动类
 *
 * @author ning
 * @date 2026/03/24
 */

@SpringBootApplication(scanBasePackages = {"com.github.forum", "com.github.common"})
@EnableScheduling
@MapperScan("com.github.forum.mapper")
@EnableFeignClients(basePackages = "com.github.forum.feign")
public class KnowledgeLinkForumApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkForumApplication.class, args);
    }
}
