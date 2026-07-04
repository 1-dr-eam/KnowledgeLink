package com.github.forum.feign;

import com.github.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "knowledgelink-user",
        contextId = "forumUserFeignClient",
        url = "http://localhost:8080"
)
public interface UserFeignClient {
    @GetMapping("/user/info")
    Result getUserInfoById(@RequestParam("id") Long id);

    @GetMapping("/user/isFollow")
    Result isFollow(@RequestParam("id") Long id);
}
