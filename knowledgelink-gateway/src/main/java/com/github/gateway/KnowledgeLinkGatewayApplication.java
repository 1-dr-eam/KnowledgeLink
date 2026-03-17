package com.github.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.github.gateway", "com.github.common"})
public class KnowledgeLinkGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkGatewayApplication.class, args);
    }
}
