package com.murder.service;

import com.murder.common.context.BaseContext;
import com.murder.dto.ArticleCommentDTO;
import com.murder.entity.Article;
import com.murder.entity.ArticleComment;
import com.murder.entity.User;
import com.murder.mapper.ArticleCommentLikeMapper;
import com.murder.mapper.ArticleCommentMapper;
import com.murder.mapper.ArticleMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.impl.ArticleCommentServiceImpl;
import com.murder.vo.ArticleCommentVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 文章评论服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("文章评论服务测试")
class ArticleCommentServiceTest {

    @Mock
    private ArticleCommentMapper commentMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleCommentLikeMapper commentLikeMapper;

    @InjectMocks
    private ArticleCommentServiceImpl commentService;

    private ArticleComment testComment;
    private ArticleComment testReply;
    private User testUser;
    private Article testArticle;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setNickname("测试用户");
        testUser.setAvatar("http://example.com/avatar.jpg");

        testArticle = new Article();
        testArticle.setId(1L);
        testArticle.setTitle("测试文章");
        testArticle.setCommentCount(5);

        testComment = new ArticleComment();
        testComment.setId(1L);
        testComment.setArticleId(1L);
        testComment.setUserId(1L);
        testComment.setContent("这是一条测试评论");
        testComment.setParentId(null);
        testComment.setLikeCount(0);
        testComment.setStatus(1);
        testComment.setCreateTime(LocalDateTime.now());

        testReply = new ArticleComment();
        testReply.setId(2L);
        testReply.setArticleId(1L);
        testReply.setUserId(2L);
        testReply.setContent("这是一条回复");
        testReply.setParentId(1L);
        testReply.setLikeCount(0);
        testReply.setStatus(1);
        testReply.setCreateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("查询评论列表测试")
    class GetCommentsTests {

        @Test
        @DisplayName("查询文章评论 - 有数据")
        void testGetCommentsByArticleId_WithData() {
            when(commentMapper.selectList(any())).thenReturn(Arrays.asList(testComment, testReply));
            when(userMapper.selectById(1L)).thenReturn(testUser);
            when(userMapper.selectById(2L)).thenReturn(testUser);
            when(commentLikeMapper.selectCount(any())).thenReturn(0L);

            List<ArticleCommentVO> result = commentService.getCommentsByArticleId(1L);

            assertNotNull(result);
            assertFalse(result.isEmpty());
            verify(commentMapper, atLeastOnce()).selectList(any());
        }

        @Test
        @DisplayName("查询文章评论 - 无数据")
        void testGetCommentsByArticleId_Empty() {
            when(commentMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<ArticleCommentVO> result = commentService.getCommentsByArticleId(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("添加评论测试")
    class AddCommentTests {

        @Test
        @DisplayName("添加一级评论 - 成功")
        void testAddComment_TopLevel_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(commentMapper.insert(any(ArticleComment.class))).thenReturn(1);
                when(articleMapper.selectById(1L)).thenReturn(testArticle);
                when(articleMapper.updateById(any(Article.class))).thenReturn(1);

                ArticleCommentDTO dto = new ArticleCommentDTO();
                dto.setArticleId(1L);
                dto.setContent("这是一条新评论");

                assertDoesNotThrow(() -> commentService.addComment(dto));
                verify(commentMapper, times(1)).insert(any(ArticleComment.class));
            }
        }

        @Test
        @DisplayName("添加回复评论 - 成功")
        void testAddComment_Reply_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(commentMapper.insert(any(ArticleComment.class))).thenReturn(1);
                when(commentMapper.selectById(1L)).thenReturn(testComment);
                when(articleMapper.selectById(1L)).thenReturn(testArticle);
                when(articleMapper.updateById(any(Article.class))).thenReturn(1);

                ArticleCommentDTO dto = new ArticleCommentDTO();
                dto.setArticleId(1L);
                dto.setContent("这是一条回复");
                dto.setParentId(1L);

                assertDoesNotThrow(() -> commentService.addComment(dto));
                verify(commentMapper, times(1)).insert(any(ArticleComment.class));
            }
        }
    }

    @Nested
    @DisplayName("删除评论测试")
    class DeleteCommentTests {

        @Test
        @DisplayName("删除评论 - 成功")
        void testDeleteComment_Success() {
            when(commentMapper.selectById(1L)).thenReturn(testComment);
            when(commentMapper.updateById(any(ArticleComment.class))).thenReturn(1);
            doNothing().when(articleMapper).decreaseCommentCount(1L);

            assertDoesNotThrow(() -> commentService.deleteComment(1L));
            verify(commentMapper, times(1)).updateById(argThat(comment ->
                    comment.getId().equals(1L) && comment.getStatus() == 0
            ));
            verify(articleMapper, times(1)).decreaseCommentCount(1L);
        }

        @Test
        @DisplayName("删除不存在的评论 - 抛出异常")
        void testDeleteComment_NotFound() {
            when(commentMapper.selectById(999L)).thenReturn(null);

            assertThrows(RuntimeException.class, () -> commentService.deleteComment(999L));
        }
    }

    @Nested
    @DisplayName("点赞评论测试")
    class LikeCommentTests {

        @Test
        @DisplayName("点赞评论 - 成功")
        void testLikeComment_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(commentLikeMapper.selectCount(any())).thenReturn(0L);
                when(commentLikeMapper.insert(any())).thenReturn(1);
                when(commentMapper.selectById(1L)).thenReturn(testComment);
                when(commentMapper.updateById(any())).thenReturn(1);

                assertDoesNotThrow(() -> commentService.likeComment(1L));
            }
        }

        @Test
        @DisplayName("取消点赞评论 - 成功")
        void testUnlikeComment_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(commentLikeMapper.delete(any())).thenReturn(1);
                when(commentMapper.selectById(1L)).thenReturn(testComment);
                when(commentMapper.updateById(any())).thenReturn(1);

                assertDoesNotThrow(() -> commentService.unlikeComment(1L));
            }
        }
    }
}
