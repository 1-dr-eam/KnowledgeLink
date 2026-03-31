package com.github.chat.feign;

import com.github.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "knowledgelink-user")
public interface UserFeignClient {
    @GetMapping("/user/getFriendList")
    Result getFriendIds();

    @GetMapping("/user/batch")
    Result getUsersByIds(@RequestParam("ids") List<Long> ids);
}
