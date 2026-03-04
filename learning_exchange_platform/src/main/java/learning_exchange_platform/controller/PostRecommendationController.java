package learning_exchange_platform.controller;

import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.mapper.UserPostBehaviorMapper;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.service.*;
import learning_exchange_platform.utils.SessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class PostRecommendationController {

    @Autowired
    private HybridPostRecommendService hybridRecommendService;

    @Autowired
    private ContentBasedPostRecommendService contentBasedService;

    @Autowired
    private CollaborativeFilteringPostRecommendService cfService;

    @Autowired
    private UserPostBehaviorMapper userPostBehaviorMapper;

    /**
     * 获取混合推荐结果
     */
    @RequestMapping("/getRecommendedPosts")
    public Result getRecommendedPosts(int limit) {
        try {
            HttpSession session= SessionUtil.getSession();
            int user_id=(int)session.getAttribute("user_id");
            List<Post> recommendations = hybridRecommendService.getHybridRecommendations(user_id, limit);
            return Result.success(recommendations);
        } catch (Exception e) {
            log.info("帖子推荐失败");
            e.printStackTrace();
            return Result.error("帖子推荐失败");
        }
    }
}
