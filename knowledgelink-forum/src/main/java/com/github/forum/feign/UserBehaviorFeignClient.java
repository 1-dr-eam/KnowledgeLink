package com.github.forum.feign;

import com.github.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "knowledgelink-user-behavior", url = "http://localhost:8080")
public interface UserBehaviorFeignClient {
    @GetMapping("/user/profile/behavior")
    Result getUserPreferenceBehavior(@RequestParam("userId") Long userId);
}
