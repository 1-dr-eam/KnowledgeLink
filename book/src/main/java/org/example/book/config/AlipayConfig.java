package org.example.book.config;

import com.alipay.easysdk.factory.Factory;
import com.alipay.easysdk.kernel.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class AlipayConfig {

    @Value("${alipay.appId}")
    private String appId;

    @Value("${alipay.merchantPrivateKey}")
    private String privateKey;

    @Value("${alipay.alipayPublicKey}")
    private String publicKey;

    @Value("${alipay.gateway}")
    private String gateway;

    @Value("${alipay.notifyUrl}")
    private String notifyUrl;

    @PostConstruct
    public void init() {
        try {
            Config config = new Config();
            config.protocol = "https";
            config.gatewayHost = this.gateway;
            config.signType = "RSA2";
            config.appId = this.appId;
            config.merchantPrivateKey = this.privateKey;
            config.alipayPublicKey = this.publicKey;
            config.notifyUrl = this.notifyUrl;
            Factory.setOptions(config);
            System.out.println("支付宝配置初始化成功");
            System.out.println("网关地址: " + config.gatewayHost);
            System.out.println("AppId: " + config.appId);
            System.out.println("通知地址: " + config.notifyUrl);
        } catch (Exception e) {
            System.err.println("支付宝配置初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}