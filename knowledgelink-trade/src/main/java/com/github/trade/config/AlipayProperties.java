package com.github.trade.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author ning
 * @date 2026/03/10
 * alipay基础信息配置类
 */
@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "alipay")
public class AlipayProperties {
    private String appId;
    private String merchantPrivateKey;
    private String alipayPublicKey;
    private String gateway;
    @Value("${alipay.withdraw.notifyUrl}")
    private String notifyUrl;
}
