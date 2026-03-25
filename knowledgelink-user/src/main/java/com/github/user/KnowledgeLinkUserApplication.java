package com.github.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.github.user", "com.github.common"})
@MapperScan("com.github.user.mapper")
public class KnowledgeLinkUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkUserApplication.class, args);
    }
}
