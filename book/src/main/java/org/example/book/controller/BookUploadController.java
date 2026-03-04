package org.example.book.controller;

import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.User;
import org.example.book.mapper.BookUserMapper;
import org.example.book.entity.Result;
import org.example.book.util.BookOSSUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Slf4j
public class BookUploadController {

    @Autowired
    private BookUserMapper bookUserMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private BookOSSUtil bookOssUtil;

    /**
     * 上传用户头像
     * @param image 头像图片文件
     * @return 上传结果，包含图片URL
     */
    @PostMapping("/upload/avatar")
    public Result uploadAvatar(@RequestParam("image") MultipartFile image) {
        try {
            // 获取HttpSession验证用户登录状态
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);

            if (session == null) {
                log.warn("用户未登录，尝试上传头像");
                return Result.error("用户未登录");
            }

            // 从session中获取用户ID
            Integer userId = (Integer) session.getAttribute("user_id");
            if (userId == null) {
                return Result.error("用户信息获取失败");
            }

            // 验证文件是否为空
            if (image.isEmpty()) {
                return Result.error("请选择要上传的图片");
            }

            // 验证文件类型（可选）
            String originalFilename = image.getOriginalFilename();
            String contentType = image.getContentType();
            if (originalFilename != null && !originalFilename.toLowerCase().matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
                return Result.error("只支持上传图片格式（jpg, jpeg, png, gif, bmp, webp）");
            }

            // 验证文件大小（限制为5MB，可选）
            long fileSize = image.getSize();
            if (fileSize > 5 * 1024 * 1024) {
                return Result.error("图片大小不能超过5MB");
            }

            log.info("用户头像上传：用户ID：{}, 文件名：{}, 文件大小：{}字节",
                    userId, originalFilename, fileSize);

            // 上传到阿里云OSS
            String url = bookOssUtil.OSSUpload(image);

            if (url == null || url.isEmpty()) {
                return Result.error("OSS上传失败");
            }

            // 更新数据库中的用户头像URL
            int result = bookUserMapper.setUserAvatar(userId, url);

            if (result > 0) {
                log.info("用户头像更新成功：用户ID={}, 头像URL={}", userId, url);

                // 构造返回数据
                Map<String, Object> data = new HashMap<>();
                data.put("url", url);
                data.put("userId", userId);
                data.put("fileName", originalFilename);
                data.put("message", "头像上传成功");

                // 使用Result.success(Object data)方法
                return Result.success(data);
            } else {
                log.error("数据库更新失败：用户ID={}, 头像URL={}", userId, url);
                return Result.error("用户头像更新失败，数据库操作异常");
            }

        } catch (Exception e) {
            log.error("用户头像上传异常：", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 检查上传状态（可选功能）
     * @return 上传服务状态
     */
    @GetMapping("/upload/status")
    public Result checkUploadStatus() {
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("service", "OSS Upload Service");
            status.put("status", "active");
            status.put("timestamp", System.currentTimeMillis());
            status.put("message", "上传服务正常");

            return Result.success(status);
        } catch (Exception e) {
            log.error("检查上传状态异常：", e);
            return Result.error("上传服务异常: " + e.getMessage());
        }
    }

    /**
     * 获取当前用户的头像URL（可选功能）
     * @return 当前用户的头像信息
     */
    @GetMapping("/avatar/current")
    public Result getCurrentUserAvatar() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);

            if (session == null) {
                return Result.error("用户未登录");
            }

            Integer userId = (Integer) session.getAttribute("user_id");
            if (userId == null) {
                return Result.error("用户信息获取失败");
            }

            // 从数据库获取用户信息
            User user = userMapper.selectUserById(userId);
            if (user == null) {
                return Result.error("用户不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("userId", userId);
            data.put("avatarUrl", user.getAvatar());
            data.put("username", user.getUsername());
            data.put("message", "获取成功");

            return Result.success(data);

        } catch (Exception e) {
            log.error("获取用户头像异常：", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }

    /**
     * 保持原有接口兼容性
     * 原来的 /upload 接口（上传头像）
     */
    @RequestMapping("/upload")
    public Result upload(MultipartFile image) {
        try {
            // 获取HttpSession
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            HttpSession session = request.getSession(false);

            if (session == null) {
                return Result.error("用户未登录");
            }

            int userId = (int) session.getAttribute("user_id");
            log.info("文件上传：用户ID：{},文件名：{}", userId, image.getOriginalFilename());

            // 上传到OSS
            String url = bookOssUtil.OSSUpload(image);

            if (url == null || url.isEmpty()) {
                return Result.error("文件上传失败");
            }

            // 更新数据库
            bookUserMapper.setUserAvatar(userId, url);

            // 构造返回数据
            Map<String, Object> data = new HashMap<>();
            data.put("url", url);
            data.put("userId", userId);

            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }
}