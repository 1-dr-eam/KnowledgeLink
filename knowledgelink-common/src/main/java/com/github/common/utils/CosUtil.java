package com.github.common.utils;

import com.github.common.config.CosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * @author ning
 * @date 2026/03/11
 * 腾讯云 COS 上传工具类
 */
@Slf4j
@Component
public class CosUtil {

    @Autowired
    private CosProperties cosProperties;

    private COSClient cosClient;

    // 允许上传的图片格式
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "webp", "bmp");

    /**
     * 初始化 COS 客户端
     */
    @PostConstruct
    public void init() {
        // 1. 初始化用户身份信息 (secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());

        // 2. 设置 bucket 的地域
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));

        // 3. 设置使用 HTTPS
        clientConfig.setHttpProtocol(HttpProtocol.https);

        // 4. 生成 cos 客户端
        this.cosClient = new COSClient(cred, clientConfig);
        log.info("腾讯云 COS 客户端初始化成功，Region: {}", cosProperties.getRegion());
    }

    /**
     * 上传图片到腾讯云 COS
     * @param file 前端传递的文件对象
     * @return 图片访问链接
     */
    public String uploadImage(MultipartFile file) {
        // 1. 基础校验
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传失败：文件为空");
        }

        // 2. 校验文件格式
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("上传失败：文件名为空");
        }
        String extension = getFileExtension(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("上传失败：不支持的图片格式。支持格式：" + ALLOWED_EXTENSIONS);
        }

        // 3. 生成存储路径：images/yyyy/MM/dd/{uuid}.{ext}
        String path = generatePath(extension);

        // 4. 执行上传
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());
            // 设置缓存控制等（可选）
            // metadata.setCacheControl("max-age=2592000");

            PutObjectRequest putObjectRequest = new PutObjectRequest(cosProperties.getBucketName(), path, inputStream, metadata);
            // 设置文件的 ACL 为 PublicRead，确保前端可以直接访问
            putObjectRequest.setCannedAcl(CannedAccessControlList.PublicRead);

            cosClient.putObject(putObjectRequest);

            log.info("图片上传成功: {}", path);

            // 5. 返回访问链接
            return getUrl(path);
        } catch (IOException e) {
            log.error("文件流读取失败", e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("COS 上传异常", e);
            throw new RuntimeException("文件上传服务异常");
        }
    }

    /**
     * 生成文件路径
     * 策略：images/年份/月份/日期/UUID.扩展名
     */
    private String generatePath(String extension) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String datePath = sdf.format(new Date());
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "images/" + datePath + "/" + uuid + "." + extension;
    }

    /**
     * 获取文件后缀名
     */
    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            return "";
        }
        return filename.substring(lastDotIndex + 1);
    }

    /**
     * 拼接完整的访问 URL
     */
    private String getUrl(String path) {
        // 如果配置了自定义域名，直接使用自定义域名
        if (cosProperties.getUrl() != null && !cosProperties.getUrl().isEmpty()) {
            // 处理末尾斜杠
            String baseUrl = cosProperties.getUrl();
            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return baseUrl + "/" + path;
        }

        // 默认 COS 域名：https://<BucketName>.cos.<Region>.myqcloud.com/<Path>
        return "https://" + cosProperties.getBucketName() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + path;
    }
}
