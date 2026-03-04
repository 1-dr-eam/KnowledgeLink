package learning_exchange_platform.controller;

import learning_exchange_platform.mapper.UserMapper;
import learning_exchange_platform.model.Result;
import learning_exchange_platform.utils.OSSUtil;
import learning_exchange_platform.utils.SessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.File;
import java.util.UUID;

@RestController
@Slf4j
public class UploadController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OSSUtil ossUtil;

    @RequestMapping("/upload")
    public Result upload(MultipartFile image) {
        try {
            HttpSession session= SessionUtil.getSession();
            int user_id = (int) session.getAttribute("user_id");
            log.info("文件上传：用户ID：{},文件名：{}",user_id,image.getOriginalFilename());

            //上传到OSS及数据库同步
            String url=ossUtil.OSSUpload(image);
            userMapper.setUserAvatar(user_id,url);
            return Result.success(url);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }

    }
}
