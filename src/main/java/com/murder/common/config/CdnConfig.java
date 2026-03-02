package com.murder.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * CDN配置类
 * 支持阿里云OSS、七牛云、腾讯云COS等CDN服务
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "cdn")
public class CdnConfig {

    /**
     * 是否启用CDN
     */
    private boolean enabled = false;

    /**
     * CDN域名（如：https://cdn.example.com）
     */
    private String domain = "";

    /**
     * 原始文件域名（本地服务器地址）
     */
    private String originDomain = "http://localhost:8080";

    /**
     * 图片处理配置
     */
    private ImageProcess imageProcess = new ImageProcess();

    /**
     * 图片处理配置内部类
     */
    @Data
    public static class ImageProcess {
        /**
         * 是否启用图片处理
         */
        private boolean enabled = true;

        /**
         * 缩略图参数（阿里云OSS格式）
         * 示例：?x-oss-process=image/resize,w_300
         */
        private String thumbnail = "?x-oss-process=image/resize,w_300,h_300,m_fill";

        /**
         * 中等尺寸图片参数
         */
        private String medium = "?x-oss-process=image/resize,w_600";

        /**
         * 大图参数
         */
        private String large = "?x-oss-process=image/resize,w_1200";

        /**
         * WebP格式转换参数
         */
        private String webp = "?x-oss-process=image/format,webp";

        /**
         * 质量压缩参数
         */
        private String quality = "?x-oss-process=image/quality,q_80";

        /**
         * 组合参数：缩略图 + WebP + 质量压缩
         */
        private String thumbnailOptimized = "?x-oss-process=image/resize,w_300,h_300,m_fill/format,webp/quality,q_80";

        /**
         * 组合参数：中等尺寸 + WebP + 质量压缩
         */
        private String mediumOptimized = "?x-oss-process=image/resize,w_600/format,webp/quality,q_85";
    }
}
