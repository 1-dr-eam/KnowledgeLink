package com.github.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ning
 * @date 2026/03/11
 * 腾讯云cos配置信息
 */
@Data
@Component
@ConfigurationProperties(prefix = "tencent.cos")
public class CosProperties {
    // 腾讯云 secretId
    private String secretId;

    // 腾讯云 SecretKey
    private String secretKey;

    // 存储桶地域
    private String region;

    // 存储桶名称
    private String bucketName;

    // 自定义域名（可选，如果不配置，将使用默认的 COS 域名）
    private String url;
}
