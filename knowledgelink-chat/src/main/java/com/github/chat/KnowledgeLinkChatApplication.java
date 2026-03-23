package com.github.chat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.github.chat", "com.github.common"})
@MapperScan("com.github.chat.mapper")
public class KnowledgeLinkChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkChatApplication.class, args);
    }
}
