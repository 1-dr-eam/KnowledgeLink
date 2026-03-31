package com.github.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 聊天服务启动类
 *
 * @author ning
 * @date 2026/03/24
 */
@SpringBootApplication(scanBasePackages = {"com.github.chat", "com.github.common"})
@MapperScan("com.github.chat.mapper")
@EnableFeignClients(basePackages = "com.github.chat.feign")
public class KnowledgeLinkChatApplication {
    /**
     * 启动聊天服务应用
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkChatApplication.class, args);
    }
}
