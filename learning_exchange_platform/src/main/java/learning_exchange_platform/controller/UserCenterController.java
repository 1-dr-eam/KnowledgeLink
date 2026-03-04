package learning_exchange_platform.controller;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.Post;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.model.User;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@RestController
public class UserCenterController {
    @Autowired
    private UserService userService;

    @RequestMapping("/userCenter/getLikeCounts")
    public Result getLikeCounts() {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");

        int count=userService.getLikeCounts(user_id);
        if(count>=0){
            return Result.success(count);
        }else {
            return Result.error("获取总点赞量失败");
        }
    }

    @RequestMapping("/userCenter/getCollectCounts")
    public Result getCollectCounts() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        int user_id=(int)session.getAttribute("user_id");

        int count=userService.getCollectCounts(user_id);
        if(count>=0){
            return Result.success(count);
        }else {
            return Result.error("获取总收藏量失败");
        }
    }

    @RequestMapping("/userCenter/getPageViews")
    public Result getPageViews() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        int user_id=(int)session.getAttribute("user_id");

        int count=userService.getPageViews(user_id);
        if(count>=0){
            return Result.success(count);
        }else {
            return Result.error("获取总浏览量失败");
        }
    }

    @RequestMapping("/userCenter/getFocusUser")
    public Result getFocusUser() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);
            int user_id=(int)session.getAttribute("user_id");
            List<User> users=userService.getFocusUser(user_id);
            return Result.success(users);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取关注列表失败");
        }
    }

    @RequestMapping("/userCenter/getFans")
    public Result getFans() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);
            int user_id=(int)session.getAttribute("user_id");
            List<User> users=userService.getFans(user_id);
            return Result.success(users);
        }catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取粉丝列表失败");
        }

    }

    @RequestMapping("/userCenter/getFriends")
    public Result getFriends() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        int user_id=(int)session.getAttribute("user_id");

        List<User> users=userService.getFriends(user_id);
        if (!users.isEmpty()) {
            return Result.success(users);
        }else {
            return Result.error("获取好友列表失败");
        }
    }

    //个人中心中搜索收藏的帖子
    @RequestMapping("/userCenter/searchCollectPosts")
    public Result searchCollectPosts(String searchKey) {
        int user_id=(int)SessionUtil.getSession().getAttribute("user_id");
        try {
            List<Post> posts=userService.searchCollectPosts(user_id,searchKey);
            return Result.success(posts);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("搜索帖子失败");
        }
    }
}
