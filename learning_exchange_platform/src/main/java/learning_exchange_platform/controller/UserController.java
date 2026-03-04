package learning_exchange_platform.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import jakarta.websocket.Session;
import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.LoginResult;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.model.User;
import learning_exchange_platform.model.UserCenterInfo;
import learning_exchange_platform.service.MessageProducer;
import learning_exchange_platform.service.UserService;
import learning_exchange_platform.utils.JWTUtil;
import learning_exchange_platform.utils.SessionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//定义请求处理类
@RestController
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private MessageProducer messageProducer;

    @RequestMapping("/login")
    public Result login(String username, String password,String securityCode) {
        if (securityCode == null) {
            // 加密密码
            String encryptedPassword = encryptPassword(password);
            User user = userService.login(username, encryptedPassword);
            if (user!=null) {
                Map<String, Object> claims=new HashMap<>();
                claims.put("user_id", user.getId());
                claims.put("username", user.getUsername());
                String jwt=JWTUtil.generateJWT(claims);
                return Result.success(jwt);
            }
            else {
                return Result.error("用户名或密码错误");
            }
        }else {
            HttpSession session= SessionUtil.getSession();
            boolean flag=CaptchaController.validateCaptcha(securityCode,session);
            if(!flag){
                return Result.error("验证码错误");
            }
            // 加密密码
            String encryptedPassword = encryptPassword(password);
            User user = userService.login(username, encryptedPassword);
            if (user!=null) {
                Map<String, Object> claims=new HashMap<>();
                claims.put("user_id", user.getId());
                claims.put("username", user.getUsername());
                String jwt=JWTUtil.generateJWT(claims);
//                LoginResult loginResult=new LoginResult(jwt,user);
                return Result.success(jwt);
            }
            else {
                return Result.error("用户名或密码错误");
            }
        }

    }

    //定义请求处理方法
    @RequestMapping("/register")
    public Result register(String phone, String username, String password, String confirmPassword,String securityCode, String grade, String major) throws Exception{
            System.out.println("执行注册操作...");
            HttpSession session= SessionUtil.getSession();
            boolean flag=CaptchaController.validateCaptcha(securityCode,session);
            if(!flag){
                return Result.error("验证码错误");
            }
            //验证码正确再执行注册操作
            // 加密密码
            if(!password.equals(confirmPassword)){
                return Result.error("两次密码不一致");
            }
            String encryptedPassword = encryptPassword(password);
            User user=new User(phone,username,encryptedPassword,grade,major);
            System.out.println("register user information:" + user);
            boolean success=userService.register(user);
            if(success){
                System.out.println("注册成功");
                Map<String, Object> claims=new HashMap<>();
                claims.put("user_id", user.getId());
                claims.put("username", user.getUsername());
                String jwt=JWTUtil.generateJWT(claims);
                return Result.success(jwt);
            }else {
                System.out.println("注册失败");
                return Result.error("注册失败");
            }
    }

    //SHA-256算法对密码进行加密存储
    private String encryptPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (int i = 0; i < encodedHash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedHash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @RequestMapping("/getUserInfo")
    public Result getUserInfo() {
        HttpSession session= SessionUtil.getSession();
        User user=userService.getUserInfo(Integer.parseInt(session.getAttribute("user_id").toString()));
        if (user!=null) {
            return Result.success(user);
        }else {
            return Result.error("获取当前用户信息失败");
        }
    }

    @RequestMapping("/getUserCenterInfoById")
    public Result getUserCenterInfoById(int user_id) {
        UserCenterInfo userCenterInfo=userService.getUserCenterInfo(user_id);
        if (userCenterInfo!=null) {
            return Result.success(userCenterInfo);
        }else {
            return Result.error("获取其他用户信息失败");
        }
    }

    @RequestMapping("/updateUser")
    public Result updateUser(String username,String grade,String major,String summary) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.updateUser(username,grade,major,summary,user_id);
        if(success){
            return Result.success("个人资料修改成功");
        }else {
            return Result.error("个人资料修改失败");
        }
    }

    @RequestMapping("/user/likePost")
    public Result likePost(int post_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.likePost(user_id,post_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("点赞帖子失败");
        }
    }

    @RequestMapping("/user/collectPost")
    public Result collectPost(int post_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.collectPost(user_id,post_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("收藏帖子失败");
        }
    }

    @RequestMapping("/user/likeComment")
    public Result likeComment(int comment_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.likeComment(user_id,comment_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("点赞评论失败");
        }
    }

    @RequestMapping("/user/focusUser")
    public Result focusUser(int focus_user_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.focusUser(user_id,focus_user_id);
        if(success){
//            //判断双方是否互关，若互关则调用这个函数进行推送
//            boolean is_friend=userService.getFriendStatus(user_id,focus_user_id);
//            if(is_friend){
//                messageProducer.sendMutualFollowEvent(user_id,focus_user_id);
//            }
            return Result.success();
        }else {
            return Result.error("关注某人失败");
        }

    }

    @RequestMapping("/user/cancelLikePost")
    public Result cancelLikePost(int post_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.cancelLikePost(user_id,post_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("取消点赞帖子失败");
        }
    }

    @RequestMapping("/user/cancelCollectPost")
    public Result cancelCollectPost(int post_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.cancelCollectPost(user_id,post_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("取消收藏帖子失败");
        }

    }

    @RequestMapping("/user/cancelLikeComment")
    public Result cancelLikeComment(int comment_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.cancelLikeComment(user_id,comment_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("取消点赞评论失败");
        }

    }

    @RequestMapping("/user/cancelFocusUser")
    public Result cancelFocusUser(int focus_user_id) {
        HttpSession session= SessionUtil.getSession();
        int user_id=(int)session.getAttribute("user_id");
        boolean success=userService.cancelFocus(user_id,focus_user_id);
        if(success){
            return Result.success();
        }else {
            return Result.error("取关某人失败");
        }

    }

    @RequestMapping("/getOutstandingCreator")
    public Result getOutstandingCreator(){
        List<User> users=userService.getOutstandingCreator();
        if(!users.isEmpty()){
            return Result.success(users);
        }else {
            return Result.error("获取优秀创作者失败");
        }
    }

    @RequestMapping("/getPostLikeStatus")
    public Result getPostLikeStatus(int post_id,HttpSession session) {
        try {
            int user_id=(int)session.getAttribute("user_id");
            boolean status=userService.getPostLikeStatus(post_id,user_id);
            return Result.success(status);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取帖子点赞状态失败");
        }
    }

    @RequestMapping("/getPostCollectStatus")
    public Result getPostCollectStatus(int post_id,HttpSession session) {
        try {
            int user_id=(int)session.getAttribute("user_id");
            boolean status=userService.getPostCollectStatus(post_id,user_id);
            return Result.success(status);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取帖子收藏状态失败");
        }
    }

    @RequestMapping("/checkUsername")
    public Result checkUsername(String username) {
        try {
            boolean exist=userService.checkUsername(username);
            return Result.success(exist);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询用户名是否存在失败");
        }
    }

    @RequestMapping("/checkPhone")
    public Result checkPhone(String phone) {
        try {
            String regex = "^1[3-9]\\d{9}$";//用正则表达式匹配标准11位电话号码格式
            boolean is_match=phone.matches(regex);
            if(is_match){
                boolean exist=userService.checkPhone(phone);
                return Result.success(exist);
            }else {
                return Result.success("电话号码格式非法，请输入正确的电话号码");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询电话是否存在失败");
        }
    }

    @RequestMapping("/checkFocusStatus")
    public Result checkFocusStatus(int check_user_id) {
        try {
            int user_id=(int)SessionUtil.getSession().getAttribute("user_id");
            boolean is_focus=userService.checkFocusStatus(check_user_id,user_id);
            return Result.success(is_focus);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("获取关注状态失败");
        }
    }

}
