package com.xuecheng.media.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置类
 * 
 * 功能：
 * 1. 读取Nacos中的OSS配置
 * 2. 创建OSS客户端Bean
 * 
 * @author 学成在线项目组
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * OSS服务地址（Endpoint）
     * 示例：oss-cn-beijing.aliyuncs.com
     */
    private String endpoint;

    /**
     * 访问密钥ID（AccessKey ID）
     */
    private String accessKeyId;

    /**
     * 访问密钥Secret（AccessKey Secret）
     */
    private String accessKeySecret;

    /**
     * 存储桶名称（Bucket Name）
     */
    private String bucketName;

    /**
     * 创建OSS客户端Bean
     * 
     * Spring容器启动时自动调用此方法，创建OSS客户端实例
     * 其他类可以通过@Autowired注入使用
     * 
     * @return OSS客户端实例
     */
    @Bean
    public OSS ossClient() {
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}
