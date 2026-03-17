package com.github.forum.service;

import com.github.common.dto.Result;

public interface IImageSynthesisService {
    Result generateImage(String prompt);
}
