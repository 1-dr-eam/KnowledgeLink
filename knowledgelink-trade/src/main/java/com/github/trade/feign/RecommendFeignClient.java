package com.github.trade.feign;

import com.github.trade.dto.FineTuningRequest;
import com.github.trade.dto.RecommendationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 书籍推荐openfeign远程调用接口
 *
 * @author ning
 * @date 2026/04/05
 */
@Component
@FeignClient(name = "knowledgelink-recommend", url = "http://10.244.193.207:8000")
public interface RecommendFeignClient {
    /**
     * @return 调用接口健康检查
     * 关注参数：JSON字符串中的status字段，值为healthy/unhealthy
     */
    @GetMapping("/health")
    String health();

    /**
     * @return 推荐书籍id列表，JSON格式
     * ID列表名：recommendations
     */
    @PostMapping("/recommend")
    String recommend(@RequestBody RecommendationRequest recommendationRequest);

    @PostMapping("/fine_tuning")
    void fineTuning(@RequestBody FineTuningRequest fineTuningRequest);
}
