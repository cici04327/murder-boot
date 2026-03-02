package com.murder.service.impl;

import com.murder.common.config.CdnConfig;
import com.murder.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 图片服务实现类
 * 提供图片URL转换、CDN地址生成等功能
 */
@Service
@Slf4j
public class ImageServiceImpl implements ImageService {

    /**
     * 将本机开发环境/历史遗留的绝对地址（如 http://localhost:8001/upload/xxx）
     * 规范化为同源相对路径（/upload/xxx）。
     */
    private String normalizeLocalhostToRelative(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }

        // 已经是相对路径或非 http，本方法不处理
        if (url.startsWith("/")) {
            return url;
        }

        // 仅处理本机 localhost / 127.0.0.1 的历史地址
        if (url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1")) {
            int pathStart = url.indexOf("/", 8); // 跳过 "http://"
            if (pathStart > 0) {
                String path = url.substring(pathStart);
                // 只对上传资源做归一化，避免误处理其它绝对地址
                if (path.startsWith("/upload/")) {
                    return path;
                }
            }
        }

        return url;
    }

    @Autowired
    private CdnConfig cdnConfig;

    @Override
    public String toCdnUrl(String localUrl) {
        if (!StringUtils.hasText(localUrl)) {
            return localUrl;
        }

        // 兼容历史数据：把 http://localhost:8001/upload/xxx 这类地址规范化为同源相对路径 /upload/xxx
        // 这样在单体 8080 或前端代理场景下都能正常访问。
        String normalizedUrl = normalizeLocalhostToRelative(localUrl);

        // CDN未启用，返回规范化后的URL
        if (!cdnConfig.isEnabled() || !StringUtils.hasText(cdnConfig.getDomain())) {
            return normalizedUrl;
        }

        // 已经是CDN地址，直接返回
        if (normalizedUrl.startsWith(cdnConfig.getDomain())) {
            return normalizedUrl;
        }

        // 如果是相对路径，添加CDN域名
        if (normalizedUrl.startsWith("/")) {
            return cdnConfig.getDomain() + normalizedUrl;
        }

        // 如果是本地服务器地址，替换为CDN地址
        String originDomain = cdnConfig.getOriginDomain();
        if (StringUtils.hasText(originDomain) && normalizedUrl.startsWith(originDomain)) {
            return normalizedUrl.replace(originDomain, cdnConfig.getDomain());
        }

        // 处理其他http开头的URL（可能是旧数据）
        if (normalizedUrl.startsWith("http://localhost") || normalizedUrl.startsWith("http://127.0.0.1")) {
            // 提取路径部分
            int pathStart = normalizedUrl.indexOf("/", 8); // 跳过 "http://" 后找第一个 "/"
            if (pathStart > 0) {
                return cdnConfig.getDomain() + normalizedUrl.substring(pathStart);
            }
        }

        return normalizedUrl;
    }

    @Override
    public String getThumbnailUrl(String imageUrl) {
        if (!isValidImageUrl(imageUrl)) {
            return imageUrl;
        }

        String cdnUrl = toCdnUrl(imageUrl);
        
        if (cdnConfig.isEnabled() && cdnConfig.getImageProcess().isEnabled()) {
            return cdnUrl + cdnConfig.getImageProcess().getThumbnail();
        }
        
        return cdnUrl;
    }

    @Override
    public String getMediumUrl(String imageUrl) {
        if (!isValidImageUrl(imageUrl)) {
            return imageUrl;
        }

        String cdnUrl = toCdnUrl(imageUrl);
        
        if (cdnConfig.isEnabled() && cdnConfig.getImageProcess().isEnabled()) {
            return cdnUrl + cdnConfig.getImageProcess().getMedium();
        }
        
        return cdnUrl;
    }

    @Override
    public String getLargeUrl(String imageUrl) {
        if (!isValidImageUrl(imageUrl)) {
            return imageUrl;
        }

        String cdnUrl = toCdnUrl(imageUrl);
        
        if (cdnConfig.isEnabled() && cdnConfig.getImageProcess().isEnabled()) {
            return cdnUrl + cdnConfig.getImageProcess().getLarge();
        }
        
        return cdnUrl;
    }

    @Override
    public String getWebpUrl(String imageUrl) {
        if (!isValidImageUrl(imageUrl)) {
            return imageUrl;
        }

        String cdnUrl = toCdnUrl(imageUrl);
        
        if (cdnConfig.isEnabled() && cdnConfig.getImageProcess().isEnabled()) {
            return cdnUrl + cdnConfig.getImageProcess().getWebp();
        }
        
        return cdnUrl;
    }

    @Override
    public String getOptimizedThumbnailUrl(String imageUrl) {
        if (!isValidImageUrl(imageUrl)) {
            return imageUrl;
        }

        String cdnUrl = toCdnUrl(imageUrl);
        
        if (cdnConfig.isEnabled() && cdnConfig.getImageProcess().isEnabled()) {
            return cdnUrl + cdnConfig.getImageProcess().getThumbnailOptimized();
        }
        
        return cdnUrl;
    }

    @Override
    public String getOptimizedMediumUrl(String imageUrl) {
        if (!isValidImageUrl(imageUrl)) {
            return imageUrl;
        }

        String cdnUrl = toCdnUrl(imageUrl);
        
        if (cdnConfig.isEnabled() && cdnConfig.getImageProcess().isEnabled()) {
            return cdnUrl + cdnConfig.getImageProcess().getMediumOptimized();
        }
        
        return cdnUrl;
    }

    @Override
    public String batchToCdnUrl(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return imageUrls;
        }

        // 支持逗号分隔的多个URL
        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::toCdnUrl)
                .collect(Collectors.joining(","));
    }

    @Override
    public boolean isCdnEnabled() {
        return cdnConfig.isEnabled() && StringUtils.hasText(cdnConfig.getDomain());
    }

    /**
     * 检查是否是有效的图片URL
     */
    private boolean isValidImageUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        
        String lowerUrl = url.toLowerCase();
        return lowerUrl.endsWith(".jpg") || 
               lowerUrl.endsWith(".jpeg") || 
               lowerUrl.endsWith(".png") || 
               lowerUrl.endsWith(".gif") || 
               lowerUrl.endsWith(".webp") ||
               lowerUrl.endsWith(".bmp") ||
               lowerUrl.contains("/upload/") ||  // 上传的文件
               lowerUrl.contains("/images/");    // 图片目录
    }
}
