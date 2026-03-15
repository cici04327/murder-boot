package com.murder.service;

import com.murder.common.context.BaseContext;
import com.murder.entity.ArticleFavorite;
import com.murder.mapper.ArticleFavoriteMapper;
import com.murder.mapper.ArticleMapper;
import com.murder.service.impl.ArticleFavoriteServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文章收藏服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("文章收藏服务测试")
class ArticleFavoriteServiceTest {

    @Mock
    private ArticleFavoriteMapper articleFavoriteMapper;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private ArticleFavoriteServiceImpl articleFavoriteService;

    private ArticleFavorite testFavorite;

    @BeforeEach
    void setUp() {
        testFavorite = new ArticleFavorite();
        testFavorite.setId(1L);
        testFavorite.setUserId(1L);
        testFavorite.setArticleId(1L);
        testFavorite.setCreateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("收藏文章测试")
    class FavoriteTests {

        @Test
        @DisplayName("收藏文章 - 成功")
        void testFavoriteArticle_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(articleFavoriteMapper.selectCount(any())).thenReturn(0L);
                when(articleFavoriteMapper.insert(any(ArticleFavorite.class))).thenReturn(1);

                assertDoesNotThrow(() -> articleFavoriteService.favoriteArticle(1L));
                verify(articleFavoriteMapper, times(1)).insert(any(ArticleFavorite.class));
            }
        }

        @Test
        @DisplayName("收藏文章 - 已收藏则抛出异常")
        void testFavoriteArticle_AlreadyFavorited() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(articleFavoriteMapper.selectCount(any())).thenReturn(1L);

                assertThrows(RuntimeException.class, () -> articleFavoriteService.favoriteArticle(1L));
                verify(articleFavoriteMapper, never()).insert(any());
            }
        }
    }

    @Nested
    @DisplayName("取消收藏测试")
    class UnfavoriteTests {

        @Test
        @DisplayName("取消收藏 - 成功")
        void testUnfavoriteArticle_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(articleFavoriteMapper.delete(any())).thenReturn(1);

                assertDoesNotThrow(() -> articleFavoriteService.unfavoriteArticle(1L));
                verify(articleFavoriteMapper, times(1)).delete(any());
            }
        }
    }

    @Nested
    @DisplayName("检查收藏状态测试")
    class IsFavoritedTests {

        @Test
        @DisplayName("已收藏 - 返回true")
        void testIsFavorited_True() {
            when(articleFavoriteMapper.selectCount(any())).thenReturn(1L);

            boolean result = articleFavoriteService.isFavorited(1L, 1L);

            assertTrue(result);
        }

        @Test
        @DisplayName("未收藏 - 返回false")
        void testIsFavorited_False() {
            when(articleFavoriteMapper.selectCount(any())).thenReturn(0L);

            boolean result = articleFavoriteService.isFavorited(1L, 1L);

            assertFalse(result);
        }
    }
}
