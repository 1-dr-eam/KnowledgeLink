package com.github.forum.service.impl;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.alibaba.dashscope.utils.JsonUtils;
import com.github.common.dto.Result;
import com.github.forum.config.DashScopeImageProperties;
import com.github.forum.service.IImageSynthesisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 图像合成服务实施
 *
 * @author ning
 * @date 2026/03/12
 */
@Service
public class ImageSynthesisServiceImpl implements IImageSynthesisService {
    @Autowired
    private DashScopeImageProperties dashScopeImageProperties;

    @Override
    public Result generateImage(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return Result.error("prompt不能为空");
        }
        if (dashScopeImageProperties.getApiKey() == null || dashScopeImageProperties.getApiKey().isBlank()) {
            return Result.error("dashscope.image.apiKey未配置");
        }
        String baseUrl = resolveBaseUrl();
        if (baseUrl == null) {
            return Result.error("dashscope.image.baseUrl配置无效");
        }
        Constants.baseHttpApiUrl = baseUrl;
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", Boolean.TRUE.equals(dashScopeImageProperties.getPromptExtend()));
        parameters.put("watermark", Boolean.TRUE.equals(dashScopeImageProperties.getWatermark()));
        parameters.put("negative_prompt", dashScopeImageProperties.getNegativePrompt() == null ? "" : dashScopeImageProperties.getNegativePrompt());
        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(dashScopeImageProperties.getApiKey().trim())
                .model(resolveModel())
                .prompt(prompt)
                .n(resolveN())
                .size(resolveSize())
                .parameters(parameters)
                .build();
        ImageSynthesis imageSynthesis = new ImageSynthesis();
        try {
            ImageSynthesisResult result = imageSynthesis.call(param);
            return Result.success(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException e) {
            return Result.error(e.getMessage());
        }
    }

    private String resolveModel() {
        if (dashScopeImageProperties.getModel() == null || dashScopeImageProperties.getModel().isBlank()) {
            return "qwen-image-plus";
        }
        return dashScopeImageProperties.getModel().trim();
    }

    private Integer resolveN() {
        if (dashScopeImageProperties.getN() == null || dashScopeImageProperties.getN() < 1) {
            return 1;
        }
        return dashScopeImageProperties.getN();
    }

    private String resolveSize() {
        if (dashScopeImageProperties.getSize() == null || dashScopeImageProperties.getSize().isBlank()) {
            return "1664*928";
        }
        return dashScopeImageProperties.getSize().trim();
    }

    private String resolveBaseUrl() {
        String raw = dashScopeImageProperties.getBaseUrl();
        if (raw == null || raw.isBlank()) {
            return "https://dashscope.aliyuncs.com/api/v1";
        }
        String normalized = raw.trim()
                .replace("`", "")
                .replace("\"", "")
                .replace("'", "");
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (!normalized.startsWith("http://") && !normalized.startsWith("https://")) {
            return null;
        }
        return normalized;
    }
}
