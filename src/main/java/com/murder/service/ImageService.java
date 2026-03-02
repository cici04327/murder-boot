package com.murder.service;

/**
 * 图片服务接口
 * 提供图片URL转换、CDN地址生成等功能
 */
public interface ImageService {

    /**
     * 将本地图片URL转换为CDN URL
     * @param localUrl 本地图片URL
     * @return CDN URL（如果CDN未启用，返回原URL）
     */
    String toCdnUrl(String localUrl);

    /**
     * 获取缩略图URL
     * @param imageUrl 原始图片URL
     * @return 缩略图URL
     */
    String getThumbnailUrl(String imageUrl);

    /**
     * 获取中等尺寸图片URL
     * @param imageUrl 原始图片URL
     * @return 中等尺寸图片URL
     */
    String getMediumUrl(String imageUrl);

    /**
     * 获取大图URL
     * @param imageUrl 原始图片URL
     * @return 大图URL
     */
    String getLargeUrl(String imageUrl);

    /**
     * 获取WebP格式图片URL
     * @param imageUrl 原始图片URL
     * @return WebP格式图片URL
     */
    String getWebpUrl(String imageUrl);

    /**
     * 获取优化后的缩略图URL（WebP + 压缩）
     * @param imageUrl 原始图片URL
     * @return 优化后的缩略图URL
     */
    String getOptimizedThumbnailUrl(String imageUrl);

    /**
     * 获取优化后的中等尺寸图片URL（WebP + 压缩）
     * @param imageUrl 原始图片URL
     * @return 优化后的中等尺寸图片URL
     */
    String getOptimizedMediumUrl(String imageUrl);

    /**
     * 批量转换图片URL为CDN URL
     * @param imageUrls 逗号分隔的图片URL字符串
     * @return 转换后的URL字符串
     */
    String batchToCdnUrl(String imageUrls);

    /**
     * 检查CDN是否启用
     * @return true-启用，false-未启用
     */
    boolean isCdnEnabled();
}
