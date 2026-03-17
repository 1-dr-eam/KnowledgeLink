package com.github.trade.config;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * @author ning
 * @date 2026/03/10
 * alipay初始化类
 */
@Configuration
public class AlipayConfig {

    private static final Logger log = LoggerFactory.getLogger(AlipayConfig.class);
    private final AlipayProperties alipayProperties;

    public AlipayConfig(AlipayProperties alipayProperties) {
        this.alipayProperties = alipayProperties;
    }

    @PostConstruct
    public void init() {
        try {
            Config config = new Config();
            config.protocol = "https";
            config.gatewayHost = alipayProperties.getGateway();
            config.signType = "RSA2";
            config.appId = alipayProperties.getAppId();
            config.merchantPrivateKey = alipayProperties.getMerchantPrivateKey();
            config.alipayPublicKey = alipayProperties.getAlipayPublicKey();
            config.notifyUrl = alipayProperties.getNotifyUrl();
            Factory.setOptions(config);
            System.out.println("支付宝配置初始化成功");
            System.out.println("网关地址: " + config.gatewayHost);
            System.out.println("AppId: " + config.appId);
            System.out.println("通知地址: " + config.notifyUrl);
        } catch (Exception e) {
            System.err.println("支付宝配置初始化失败: " + e.getMessage());
            log.error(e.getMessage(), e);
        }
    }
}
