package com.github.forum.service;

import com.github.common.dto.Result;
import com.github.forum.KnowledgeLinkForumApplication;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = KnowledgeLinkForumApplication.class, properties = {
        "spring.cloud.nacos.enabled=false",
        "spring.cloud.nacos.config.enabled=false",
        "spring.cloud.nacos.discovery.enabled=false"
})
class ImageSynthesisServiceTest {
    @Autowired
    private IImageSynthesisService imageSynthesisService;

    @Value("${dashscope.image.apiKey:${DASHSCOPE_API_KEY:}}")
    private String apiKey;
    @Value("${test.dashscope.integration.enabled:false}")
    private Boolean integrationEnabled;

    @Test
    void generateImage() {
        Assumptions.assumeTrue(Boolean.TRUE.equals(integrationEnabled), "未开启test.dashscope.integration.enabled");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(), "未配置dashscope.image.apiKey或DASHSCOPE_API_KEY");
        Result result = imageSynthesisService.generateImage("一个科技感十足的知识社区logo，扁平风格，蓝紫配色");
        assertNotNull(result);
        assertEquals(1, result.getCode(), "调用失败，msg=" + result.getMsg() + ", data=" + result.getData());
        assertNotNull(result.getData());
    }
}
