package com.github.forum.service.recommend;

import cn.hutool.json.JSONUtil;
import com.github.common.dto.Result;
import com.github.forum.feign.UserBehaviorFeignClient;
import com.github.forum.feign.dto.UserBehaviorProfileDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class FeignUserBehaviorProfileProvider implements UserBehaviorProfileProvider {
    private final UserBehaviorFeignClient userBehaviorFeignClient;

    public FeignUserBehaviorProfileProvider(UserBehaviorFeignClient userBehaviorFeignClient) {
        this.userBehaviorFeignClient = userBehaviorFeignClient;
    }

    @Override
    public UserInterestProfile getUserInterestProfile(Long userId) {
        if (userId == null) {
            return new UserInterestProfile(new ArrayList<>(), new ArrayList<>());
        }
        try {
            Result result = userBehaviorFeignClient.getUserPreferenceBehavior(userId);
            if (result == null || result.getCode() == null || result.getCode() != 1 || result.getData() == null) {
                return new UserInterestProfile(new ArrayList<>(), new ArrayList<>());
            }
            UserBehaviorProfileDTO profile = JSONUtil.toBean(JSONUtil.parseObj(result.getData()), UserBehaviorProfileDTO.class);
            return new UserInterestProfile(splitTokens(profile.getUserCategories()), splitTokens(profile.getUserKeywords()));
        } catch (Exception e) {
            return new UserInterestProfile(new ArrayList<>(), new ArrayList<>());
        }
    }

    private List<String> splitTokens(String source) {
        if (source == null || source.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(source.split("[,，\\s]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .distinct()
                .limit(20)
                .collect(Collectors.toList());
    }
}
