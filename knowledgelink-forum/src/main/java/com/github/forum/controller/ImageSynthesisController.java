package com.github.forum.controller;

import com.github.common.dto.Result;
import com.github.forum.service.IImageSynthesisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/image")
public class ImageSynthesisController {
    @Autowired
    private IImageSynthesisService imageSynthesisService;

    @PostMapping("/generate")
    public Result generate(@RequestParam("prompt") String prompt) {
        return imageSynthesisService.generateImage(prompt);
    }
}
