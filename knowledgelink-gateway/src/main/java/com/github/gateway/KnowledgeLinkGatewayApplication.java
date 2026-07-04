package com.github.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
        basePackages = {"com.github.gateway", "com.github.common"},
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = "com\\.github\\.common\\.config\\.(UserContextInterceptor|WebMvcConfig)|com\\.github\\.common\\.utils\\.CosUtil"
        )
)
public class KnowledgeLinkGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(KnowledgeLinkGatewayApplication.class, args);
    }
}
