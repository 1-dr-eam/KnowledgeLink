package com.github.forum.service;

import com.github.common.dto.Result;

/**
 * IImageSynthesisService 服务接口
 *
 * @author ning
 * @date 2026/03/24
 */

public interface IImageSynthesisService {
    Result generateImage(String prompt);
}
