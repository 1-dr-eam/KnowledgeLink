package org.example.book.util;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Component
public class BookOSSUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.bucket-name}")
    private String bucketName;

    @Value("${aliyun.oss.region}")
    private String region;

    @Value("${aliyun.oss.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret}")
    private String accessKeySecret;

    /**
     * 上传文件到阿里云OSS
     * @param file 要上传的文件
     * @return 文件的访问URL
     * @throws Exception 上传异常
     */
    public String OSSUpload(MultipartFile file) throws Exception {
        // 使用配置文件中的AccessKey创建凭证提供者
        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory
                .newDefaultCredentialProvider(accessKeyId, accessKeySecret);

        // 生成唯一文件名，防止文件重名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // 创建OSSClient实例
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            InputStream inputStream = file.getInputStream();

            // 创建PutObjectRequest对象
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, fileName, inputStream);

            // 上传文件到OSS
            PutObjectResult result = ossClient.putObject(putObjectRequest);

            // 构建文件访问URL
            // 格式: https://bucket-name.endpoint/file-name
            String url = "https://" + bucketName + "." + endpoint.replace("https://", "") + "/" + fileName;
            return url;

        } catch (OSSException oe) {
            System.out.println("阿里云OSS异常:");
            System.out.println("错误信息: " + oe.getErrorMessage());
            System.out.println("错误代码: " + oe.getErrorCode());
            System.out.println("请求ID: " + oe.getRequestId());
            System.out.println("主机ID: " + oe.getHostId());
            throw new Exception("文件上传失败: " + oe.getErrorMessage(), oe);

        } catch (ClientException ce) {
            System.out.println("客户端异常:");
            System.out.println("错误信息: " + ce.getMessage());
            throw new Exception("文件上传失败: " + ce.getMessage(), ce);

        } catch (Exception e) {
            System.out.println("上传文件时发生异常: " + e.getMessage());
            throw e;

        } finally {
            // 关闭OSSClient
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 上传文件并指定OSS中的存储路径
     * @param file 要上传的文件
     * @param folder OSS中的文件夹路径（如：images/）
     * @return 文件的访问URL
     * @throws Exception 上传异常
     */
    public String OSSUpload(MultipartFile file, String folder) throws Exception {
        DefaultCredentialProvider credentialsProvider = CredentialsProviderFactory
                .newDefaultCredentialProvider(accessKeyId, accessKeySecret);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = UUID.randomUUID().toString() + fileExtension;

        // 添加文件夹路径
        String objectName = folder.endsWith("/") ? folder + fileName : folder + "/" + fileName;

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            InputStream inputStream = file.getInputStream();
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            ossClient.putObject(putObjectRequest);

            String url = "https://" + bucketName + "." + endpoint.replace("https://", "") + "/" + objectName;
            return url;

        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 获取配置信息（用于测试）
     */
    public String getConfigInfo() {
        return String.format("OSS配置信息: endpoint=%s, bucket=%s, region=%s",
                endpoint, bucketName, region);
    }
}