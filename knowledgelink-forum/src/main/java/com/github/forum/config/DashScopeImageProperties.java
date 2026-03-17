package com.github.forum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 仪表盘范围图像属性
 *
 * @author ning
 * @date 2026/03/12
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "dashscope.image")
public class DashScopeImageProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Integer n;
    private String size;
    private String negativePrompt;
    private Boolean promptExtend;
    private Boolean watermark;
}
