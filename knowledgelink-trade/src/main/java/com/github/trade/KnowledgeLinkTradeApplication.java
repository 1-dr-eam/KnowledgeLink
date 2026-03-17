package com.github.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author ning
 * @date 2026/03/16
 */
@SpringBootApplication(scanBasePackages = {"com.github.trade", "com.github.common"})
@MapperScan("com.github.trade.mapper")
public class KnowledgeLinkTradeApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkTradeApplication.class, args);
    }
}
