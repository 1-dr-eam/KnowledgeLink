package com.github.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关属性配置
 *
 * @author ning
 * @date 2026/03/23
 */
@Data
@Component
@ConfigurationProperties(prefix = "gateway.auth")
public class GatewayAuthProperties {
    private boolean enabled = true;
    private String headerName = "Authorization";
    private String tokenPrefix = "Bearer";
    private List<String> whitelist = new ArrayList<>();
}
