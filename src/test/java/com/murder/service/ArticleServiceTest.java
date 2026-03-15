package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ArticleDTO;
import com.murder.entity.Article;
import com.murder.entity.ArticleLike;
import com.murder.entity.User;
import com.murder.mapper.*;
import com.murder.service.impl.ArticleServiceImpl;
import com.murder.vo.ArticleVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文章服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("文章服务测试")
class ArticleServiceTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleLikeMapper articleLikeMapper;

    @Mock
    private ArticleFavoriteMapper articleFavoriteMapper;

    @Mock
    private ArticleCommentMapper articleCommentMapper;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private Article testArticle;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setNickname("测试用户");
        testUser.setAvatar("http://example.com/avatar.jpg");

        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("测试文章标题");
        testArticle.setContent("这是测试文章的内容，内容非常丰富");
        testArticle.setSummary("文章摘要");
        testArticle.setCoverImage("http://example.com/cover.jpg");
        testArticle.setAuthorId(1L);
        testArticle.setCategory(1);
        testArticle.setStatus(1);
        testArticle.setViewCount(100);
        testArticle.setLikeCount(10);
        testArticle.setCommentCount(5);
        testArticle.setFavoriteCount(3);
        testArticle.setCreateTime(LocalDateTime.now());
        testArticle.setUpdateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("查询文章详情测试")
    class GetByIdTests {

        @Test
        @DisplayName("根据ID查询文章 - 成功")
        void testGetById_Success() {
            when(articleMapper.selectById(1L)).thenReturn(testArticle);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            ArticleVO result = articleService.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("测试文章标题", result.getTitle());
            verify(articleMapper, times(1)).selectById(1L);
        }

        @Test
        @DisplayName("根据ID查询文章 - 不存在返回null")
        void testGetById_NotFound() {
            when(articleMapper.selectById(999L)).thenReturn(null);

            ArticleVO result = articleService.getById(999L);

            assertNull(result);
            verify(articleMapper, times(1)).selectById(999L);
        }
    }

    @Nested
    @DisplayName("分页查询测试")
    class PageQueryTests {

        @Test
        @DisplayName("分页查询 - 成功返回数据")
        void testPageQuery_Success() {
            List<Article> articles = Collections.singletonList(testArticle);
            when(articleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(articleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<Article> p = inv.getArgument(0);
                        p.setRecords(articles);
                        p.setTotal(1);
                        return p;
                    });
            when(userMapper.selectById(anyLong())).thenReturn(testUser);

            PageResult<ArticleVO> result = articleService.pageQuery(1, 10, null, null, 1, "time");

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertFalse(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("分页查询 - 空结果")
        void testPageQuery_Empty() {
            when(articleMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(articleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<Article> p = inv.getArgument(0);
                        p.setRecords(Collections.emptyList());
                        p.setTotal(0);
                        return p;
                    });

            PageResult<ArticleVO> result = articleService.pageQuery(1, 10, null, null, null, null);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("新增/更新/删除测试")
    class CrudTests {

        @Test
        @DisplayName("新增文章 - 成功")
        void testAdd_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(articleMapper.insert(any(Article.class))).thenReturn(1);
                ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);

                ArticleDTO dto = new ArticleDTO();
                dto.setTitle("新文章");
                dto.setContent("内容");
                dto.setCategory(1);
                dto.setStatus(1);

                articleService.add(dto);
                verify(articleMapper, times(1)).insert(articleCaptor.capture());

                Article created = articleCaptor.getValue();
                assertEquals("新文章", created.getTitle());
                assertEquals("内容", created.getContent());
                assertEquals(1, created.getCategory());
                assertEquals("新手攻略", created.getCategoryName());
                assertEquals(0, created.getViewCount());
                assertEquals(0, created.getLikeCount());
                assertNotNull(created.getPublishTime());
            }
        }

        @Test
        @DisplayName("更新文章 - 成功")
        void testUpdate_Success() {
            Article oldArticle = new Article();
            oldArticle.setId(1L);
            oldArticle.setStatus(0);
            when(articleMapper.selectById(1L)).thenReturn(oldArticle);
            when(articleMapper.updateById(any(Article.class))).thenReturn(1);
            ArgumentCaptor<Article> articleCaptor = ArgumentCaptor.forClass(Article.class);

            ArticleDTO dto = new ArticleDTO();
            dto.setId(1L);
            dto.setTitle("更新标题");
            dto.setContent("更新内容");
            dto.setCategory(2);
            dto.setStatus(1);

            articleService.update(dto);
            verify(articleMapper, times(1)).updateById(articleCaptor.capture());

            Article updated = articleCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals("更新标题", updated.getTitle());
            assertEquals("更新内容", updated.getContent());
            assertEquals(2, updated.getCategory());
            assertEquals("选本技巧", updated.getCategoryName());
            assertNotNull(updated.getPublishTime());
        }

        @Test
        @DisplayName("删除文章 - 成功")
        void testDelete_Success() {
            when(articleMapper.deleteById(1L)).thenReturn(1);

            articleService.delete(1L);
            verify(articleMapper, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("热门/推荐文章测试")
    class HotRecommendTests {

        @Test
        @DisplayName("获取热门文章")
        void testGetHotArticles() {
            when(articleMapper.selectList(any())).thenReturn(Collections.singletonList(testArticle));
            when(userMapper.selectById(anyLong())).thenReturn(testUser);

            List<ArticleVO> result = articleService.getHotArticles(5);

            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("获取推荐文章")
        void testGetRecommendedArticles() {
            when(articleMapper.selectList(any())).thenReturn(Collections.singletonList(testArticle));
            when(userMapper.selectById(anyLong())).thenReturn(testUser);

            List<ArticleVO> result = articleService.getRecommendedArticles(5);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("浏览/点赞测试")
    class InteractionTests {

        @Test
        @DisplayName("增加浏览次数")
        void testIncreaseViewCount() {
            doNothing().when(articleMapper).increaseViewCount(1L);

            articleService.increaseViewCount(1L);

            verify(articleMapper, times(1)).increaseViewCount(1L);
        }

        @Test
        @DisplayName("点赞文章 - 成功")
        void testLikeArticle_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(articleLikeMapper.selectCount(any())).thenReturn(0L);
                when(articleLikeMapper.insert(any())).thenReturn(1);
                doNothing().when(articleMapper).increaseLikeCount(1L);
                ArgumentCaptor<ArticleLike> likeCaptor = ArgumentCaptor.forClass(ArticleLike.class);

                articleService.likeArticle(1L);

                verify(articleLikeMapper, times(1)).insert(likeCaptor.capture());
                verify(articleMapper, times(1)).increaseLikeCount(1L);

                ArticleLike articleLike = likeCaptor.getValue();
                assertEquals(1L, articleLike.getArticleId());
                assertEquals(1L, articleLike.getUserId());
            }
        }

        @Test
        @DisplayName("取消点赞文章 - 成功")
        void testUnlikeArticle_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(articleLikeMapper.delete(any())).thenReturn(1);
                doNothing().when(articleMapper).decreaseLikeCount(1L);

                articleService.unlikeArticle(1L);

                verify(articleLikeMapper, times(1)).delete(any());
                verify(articleMapper, times(1)).decreaseLikeCount(1L);
            }
        }

        @Test
        @DisplayName("检查是否已点赞")
        void testIsLiked() {
            when(articleLikeMapper.selectCount(any())).thenReturn(1L);

            boolean result = articleService.isLiked(1L, 1L);

            assertTrue(result);
        }

        @Test
        @DisplayName("检查是否已点赞 - 未点赞")
        void testIsLiked_NotLiked() {
            when(articleLikeMapper.selectCount(any())).thenReturn(0L);

            boolean result = articleService.isLiked(1L, 1L);

            assertFalse(result);
        }
    }
}
