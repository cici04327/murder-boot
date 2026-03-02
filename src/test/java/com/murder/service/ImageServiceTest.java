package com.murder.service;

import com.murder.common.config.CdnConfig;
import com.murder.service.impl.ImageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * 图片服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("图片服务测试")
class ImageServiceTest {

    @Mock
    private CdnConfig cdnConfig;

    @Mock
    private CdnConfig.ImageProcess imageProcess;

    @InjectMocks
    private ImageServiceImpl imageService;

    @BeforeEach
    void setUp() {
        // 设置默认的图片处理配置
        when(cdnConfig.getImageProcess()).thenReturn(imageProcess);
        when(imageProcess.getThumbnail()).thenReturn("?x-oss-process=image/resize,w_300,h_300,m_fill");
        when(imageProcess.getMedium()).thenReturn("?x-oss-process=image/resize,w_600");
        when(imageProcess.getLarge()).thenReturn("?x-oss-process=image/resize,w_1200");
        when(imageProcess.getWebp()).thenReturn("?x-oss-process=image/format,webp");
        when(imageProcess.getThumbnailOptimized()).thenReturn("?x-oss-process=image/resize,w_300,h_300,m_fill/format,webp/quality,q_80");
        when(imageProcess.getMediumOptimized()).thenReturn("?x-oss-process=image/resize,w_600/format,webp/quality,q_85");
    }

    @Test
    @DisplayName("CDN未启用时返回原始URL")
    void testToCdnUrl_CdnDisabled() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(false);
        String localUrl = "http://localhost:8080/upload/images/2025/01/test.jpg";

        // When
        String result = imageService.toCdnUrl(localUrl);

        // Then
        assertEquals(localUrl, result);
    }

    @Test
    @DisplayName("CDN启用时转换URL")
    void testToCdnUrl_CdnEnabled() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        String localUrl = "http://localhost:8080/upload/images/2025/01/test.jpg";

        // When
        String result = imageService.toCdnUrl(localUrl);

        // Then
        assertEquals("https://cdn.example.com/upload/images/2025/01/test.jpg", result);
    }

    @Test
    @DisplayName("相对路径添加CDN域名")
    void testToCdnUrl_RelativePath() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        String relativePath = "/upload/images/2025/01/test.jpg";

        // When
        String result = imageService.toCdnUrl(relativePath);

        // Then
        assertEquals("https://cdn.example.com/upload/images/2025/01/test.jpg", result);
    }

    @Test
    @DisplayName("空URL返回原值")
    void testToCdnUrl_EmptyUrl() {
        // When
        String result = imageService.toCdnUrl("");

        // Then
        assertEquals("", result);
    }

    @Test
    @DisplayName("null URL返回null")
    void testToCdnUrl_NullUrl() {
        // When
        String result = imageService.toCdnUrl(null);

        // Then
        assertNull(result);
    }

    @Test
    @DisplayName("获取缩略图URL - CDN启用")
    void testGetThumbnailUrl_CdnEnabled() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        when(imageProcess.isEnabled()).thenReturn(true);
        String imageUrl = "http://localhost:8080/upload/images/test.jpg";

        // When
        String result = imageService.getThumbnailUrl(imageUrl);

        // Then
        assertTrue(result.contains("cdn.example.com"));
        assertTrue(result.contains("x-oss-process"));
    }

    @Test
    @DisplayName("获取缩略图URL - CDN未启用")
    void testGetThumbnailUrl_CdnDisabled() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(false);
        String imageUrl = "http://localhost:8080/upload/images/test.jpg";

        // When
        String result = imageService.getThumbnailUrl(imageUrl);

        // Then
        assertEquals(imageUrl, result);
    }

    @Test
    @DisplayName("获取中等尺寸URL")
    void testGetMediumUrl() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        when(imageProcess.isEnabled()).thenReturn(true);
        String imageUrl = "http://localhost:8080/upload/images/test.jpg";

        // When
        String result = imageService.getMediumUrl(imageUrl);

        // Then
        assertTrue(result.contains("resize,w_600"));
    }

    @Test
    @DisplayName("获取大图URL")
    void testGetLargeUrl() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        when(imageProcess.isEnabled()).thenReturn(true);
        String imageUrl = "http://localhost:8080/upload/images/test.jpg";

        // When
        String result = imageService.getLargeUrl(imageUrl);

        // Then
        assertTrue(result.contains("resize,w_1200"));
    }

    @Test
    @DisplayName("获取WebP格式URL")
    void testGetWebpUrl() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        when(imageProcess.isEnabled()).thenReturn(true);
        String imageUrl = "http://localhost:8080/upload/images/test.jpg";

        // When
        String result = imageService.getWebpUrl(imageUrl);

        // Then
        assertTrue(result.contains("format,webp"));
    }

    @Test
    @DisplayName("获取优化缩略图URL")
    void testGetOptimizedThumbnailUrl() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        when(imageProcess.isEnabled()).thenReturn(true);
        String imageUrl = "http://localhost:8080/upload/images/test.jpg";

        // When
        String result = imageService.getOptimizedThumbnailUrl(imageUrl);

        // Then
        assertTrue(result.contains("quality,q_80"));
    }

    @Test
    @DisplayName("批量转换URL")
    void testBatchToCdnUrl() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");
        when(cdnConfig.getOriginDomain()).thenReturn("http://localhost:8080");
        String urls = "http://localhost:8080/upload/1.jpg,http://localhost:8080/upload/2.jpg";

        // When
        String result = imageService.batchToCdnUrl(urls);

        // Then
        assertTrue(result.contains("cdn.example.com"));
        assertEquals(2, result.split(",").length);
    }

    @Test
    @DisplayName("批量转换URL - 空字符串")
    void testBatchToCdnUrl_Empty() {
        // When
        String result = imageService.batchToCdnUrl("");

        // Then
        assertEquals("", result);
    }

    @Test
    @DisplayName("检查CDN是否启用")
    void testIsCdnEnabled() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(true);
        when(cdnConfig.getDomain()).thenReturn("https://cdn.example.com");

        // When
        boolean result = imageService.isCdnEnabled();

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查CDN未启用")
    void testIsCdnEnabled_False() {
        // Given
        when(cdnConfig.isEnabled()).thenReturn(false);

        // When
        boolean result = imageService.isCdnEnabled();

        // Then
        assertFalse(result);
    }
}
