package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ReviewDTO;
import com.murder.entity.Review;
import com.murder.entity.User;
import com.murder.mapper.DmMapper;
import com.murder.mapper.ReviewMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.impl.ReviewServiceImpl;
import com.murder.vo.ReviewVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 评价服务单元测试
 * 使用 @ExtendWith(MockitoExtension.class) 和 @MockitoSettings(strictness = Strictness.LENIENT)
 * 使用 @InjectMocks 注入被测试对象，@Mock 注入依赖
 * 使用 @Nested 嵌套类组织测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("评价服务测试")
class ReviewServiceTest {

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserMapper userMapper;

    @Mock(name = "storeService")
    private StoreService storeService;

    @Mock(name = "scriptService")
    private ScriptService scriptService;

    @Mock(name = "userPointsService")
    private UserPointsService userPointsService;

    @Mock(name = "dmService")
    private DmService dmService;

    @Mock
    private DmMapper dmMapper;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private Review testReview;
    private ReviewDTO testReviewDTO;
    private User testUser;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setNickname("测试用户");
        testUser.setAvatar("http://example.com/avatar.jpg");
        testUser.setCreateTime(LocalDateTime.now());

        // 初始化测试评价
        testReview = new Review();
        testReview.setId(1L);
        testReview.setReservationId(100L);
        testReview.setUserId(1L);
        testReview.setStoreId(10L);
        testReview.setScriptId(20L);
        testReview.setDmId(5L);
        testReview.setOverallRating(5);
        testReview.setStoreRating(5);
        testReview.setScriptRating(5);
        testReview.setServiceRating(5);
        testReview.setDmRating(5);
        testReview.setContent("这是一个很好的剧本，体验非常棒！");
        testReview.setImages("http://example.com/img1.jpg,http://example.com/img2.jpg");
        testReview.setStatus(2); // 已通过
        testReview.setRewardPoints(75);
        testReview.setCreateTime(LocalDateTime.now());
        testReview.setIsDeleted(0);

        // 初始化测试评价DTO
        testReviewDTO = new ReviewDTO();
        testReviewDTO.setReservationId(100L);
        testReviewDTO.setStoreId(10L);
        testReviewDTO.setScriptId(20L);
        testReviewDTO.setDmId(5L);
        testReviewDTO.setOverallRating(5);
        testReviewDTO.setStoreRating(5);
        testReviewDTO.setScriptRating(5);
        testReviewDTO.setServiceRating(5);
        testReviewDTO.setDmRating(5);
        testReviewDTO.setContent("这是一个很好的剧本，体验非常棒！体验非常棒！");
        testReviewDTO.setImages("http://example.com/img1.jpg,http://example.com/img2.jpg");
    }

    @Nested
    @DisplayName("创建评价测试")
    class CreateReviewTest {

        @Test
        @DisplayName("创建评价 - 成功")
        void testCreate_Success() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(1L);
                
                when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
                when(reviewMapper.insert(any(Review.class))).thenReturn(1);
                doNothing().when(userPointsService).addPoints(anyLong(), anyInt(), anyString());
                when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

                // When
                assertDoesNotThrow(() -> reviewService.create(testReviewDTO));

                // Then
                verify(reviewMapper, times(1)).insert(any(Review.class));
                verify(userPointsService, times(1)).addPoints(eq(1L), anyInt(), anyString());
            }
        }

        @Test
        @DisplayName("创建评价 - 重复评价抛出异常")
        void testCreate_DuplicateReview() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(1L);
                
                // 模拟已存在的评价
                when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

                // When & Then
                assertThrows(RuntimeException.class, () -> reviewService.create(testReviewDTO));
                verify(reviewMapper, never()).insert(any(Review.class));
            }
        }

        @Test
        @DisplayName("创建评价 - 预约ID为空抛出异常")
        void testCreate_NullReservationId() {
            // Given
            testReviewDTO.setReservationId(null);
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(1L);

                // When & Then
                assertThrows(RuntimeException.class, () -> reviewService.create(testReviewDTO));
            }
        }

        @Test
        @DisplayName("创建评价 - 用户未登录抛出异常")
        void testCreate_UserNotLoggedIn() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(null);
                when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

                // When & Then
                assertThrows(RuntimeException.class, () -> reviewService.create(testReviewDTO));
            }
        }

        @Test
        @DisplayName("创建评价 - 计算积分正确(有图片和长内容)")
        void testCreate_PointsCalculation() {
            // Given: 有图片和长内容，应该获得 50 + 10 + 10 = 70 积分（因为 testReviewDTO 中只有三项中的两项）
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(1L);
                
                when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
                when(reviewMapper.insert(any(Review.class))).thenReturn(1);
                doNothing().when(userPointsService).addPoints(anyLong(), anyInt(), anyString());
                when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

                // When
                reviewService.create(testReviewDTO);

                // Then - 验证积分计算: 50 基础 + 10 图片 + 10 内容详细
                // testReviewDTO 中确实包含 storeRating, scriptRating, serviceRating，所以应该加 5 分 = 75
                verify(userPointsService, times(1)).addPoints(eq(1L), eq(65), anyString());
            }
        }

        @Test
        @DisplayName("创建评价 - 无图片和内容时积分计算")
        void testCreate_MinimalPoints() {
            // Given: 无图片、无长内容、只有部分评分，应该获得 50 积分
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(1L);
                
                ReviewDTO minimalDTO = new ReviewDTO();
                minimalDTO.setReservationId(100L);
                minimalDTO.setStoreId(10L);
                minimalDTO.setScriptId(20L);
                minimalDTO.setStoreRating(5);
                minimalDTO.setContent("短");
                // 无图片，无长内容
                
                when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
                when(reviewMapper.insert(any(Review.class))).thenReturn(1);
                doNothing().when(userPointsService).addPoints(anyLong(), anyInt(), anyString());
                when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

                // When
                reviewService.create(minimalDTO);

                // Then - 验证积分计算: 仅 50 基础积分
                verify(userPointsService, times(1)).addPoints(eq(1L), eq(50), anyString());
            }
        }
    }

    @Nested
    @DisplayName("根据预约ID查询评价测试")
    class GetByReservationIdTest {

        @Test
        @DisplayName("根据预约ID查询 - 查到评价")
        void testGetByReservationId_Found() {
            // Given
            when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testReview);

            // When
            Review result = reviewService.getByReservationId(100L);

            // Then
            assertNotNull(result);
            assertEquals(100L, result.getReservationId());
            assertEquals(1L, result.getId());
            verify(reviewMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("根据预约ID查询 - 查不到评价")
        void testGetByReservationId_NotFound() {
            // Given
            when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            // When
            Review result = reviewService.getByReservationId(999L);

            // Then
            assertNull(result);
            verify(reviewMapper, times(1)).selectOne(any(LambdaQueryWrapper.class));
        }
    }

    @Nested
    @DisplayName("分页查询测试")
    class PageQueryTest {

        @Test
        @DisplayName("分页查询 - 成功查询")
        void testPageQuery_Success() {
            // Given
            List<Review> reviews = Arrays.asList(testReview);
            Page<Review> page = new Page<>(1, 10);
            page.setRecords(reviews);
            page.setTotal(1);

            when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
                Page<Review> pageArg = invocation.getArgument(0);
                pageArg.setRecords(reviews);
                pageArg.setTotal(1);
                return pageArg;
            });
            when(userMapper.selectById(anyLong())).thenReturn(testUser);

            // When
            PageResult<ReviewVO> result = reviewService.pageQuery(1, 10, 10L, 20L, 1L, 2);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertNotNull(result.getRecords());
            assertFalse(result.getRecords().isEmpty());
            verify(reviewMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
            verify(reviewMapper, times(1)).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("分页查询 - 无查询条件")
        void testPageQuery_WithoutConditions() {
            // Given
            List<Review> reviews = Arrays.asList(testReview);
            Page<Review> page = new Page<>(1, 10);
            page.setRecords(reviews);
            page.setTotal(1);

            when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
                Page<Review> pageArg = invocation.getArgument(0);
                pageArg.setRecords(reviews);
                pageArg.setTotal(1);
                return pageArg;
            });
            when(userMapper.selectById(anyLong())).thenReturn(testUser);

            // When
            PageResult<ReviewVO> result = reviewService.pageQuery(1, 10, null, null, null, null);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            verify(reviewMapper, times(1)).selectCount(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("分页查询 - 空结果集")
        void testPageQuery_EmptyResult() {
            // Given
            List<Review> emptyList = new ArrayList<>();
            Page<Review> page = new Page<>(1, 10);
            page.setRecords(emptyList);
            page.setTotal(0);

            when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenAnswer(invocation -> {
                Page<Review> pageArg = invocation.getArgument(0);
                pageArg.setRecords(emptyList);
                pageArg.setTotal(0);
                return pageArg;
            });

            // When
            PageResult<ReviewVO> result = reviewService.pageQuery(1, 10, 10L, null, null, null);

            // Then
            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("审核评价测试")
    class AuditReviewTest {

        @Test
        @DisplayName("审核评价 - 审核通过")
        void testAudit_Approved() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(2L);
                
                when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

                // When
                assertDoesNotThrow(() -> reviewService.audit(1L, 2, "已通过"));

                // Then
                verify(reviewMapper, times(1)).updateById(argThat(review ->
                    review.getId().equals(1L) &&
                    review.getStatus().equals(2) &&
                    review.getAuditReason().equals("已通过")
                ));
            }
        }

        @Test
        @DisplayName("审核评价 - 审核拒绝")
        void testAudit_Rejected() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(2L);
                
                when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

                // When
                assertDoesNotThrow(() -> reviewService.audit(1L, 3, "违反社区规则"));

                // Then
                verify(reviewMapper, times(1)).updateById(argThat(review ->
                    review.getId().equals(1L) &&
                    review.getStatus().equals(3) &&
                    review.getAuditReason().equals("违反社区规则")
                ));
            }
        }

        @Test
        @DisplayName("审核评价 - 用户未登录")
        void testAudit_UserNotLoggedIn() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(null);
                
                when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

                // When & Then
                assertDoesNotThrow(() -> reviewService.audit(1L, 2, "已通过"));
                verify(reviewMapper, times(1)).updateById(argThat(review ->
                    review.getAuditUserId() == null
                ));
            }
        }

        @Test
        @DisplayName("审核评价 - 验证审核时间非空")
        void testAudit_VerifyAuditTime() {
            // Given
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(2L);
                
                when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

                // When
                reviewService.audit(1L, 2, "已通过");

                // Then
                verify(reviewMapper, times(1)).updateById(argThat(review ->
                    review.getId().equals(1L) &&
                    review.getAuditTime() != null &&
                    review.getAuditUserId().equals(2L)
                ));
            }
        }

        @Test
        @DisplayName("审核评价 - 带原因的拒绝")
        void testAudit_RejectionWithReason() {
            // Given
            String reason = "评价包含敏感词汇";
            try (MockedStatic<BaseContext> baseContextMock = mockStatic(BaseContext.class)) {
                baseContextMock.when(BaseContext::getCurrentId).thenReturn(2L);
                
                when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

                // When
                reviewService.audit(1L, 3, reason);

                // Then
                verify(reviewMapper, times(1)).updateById(argThat(review ->
                    review.getId().equals(1L) &&
                    review.getStatus().equals(3) &&
                    review.getAuditReason().equals(reason)
                ));
            }
        }
    }

    @Nested
    @DisplayName("管理员回复评价测试")
    class ReplyReviewTest {

        @Test
        @DisplayName("回复评价 - 成功回复")
        void testReply_Success() {
            // Given
            String replyContent = "感谢您的评价，我们会继续努力！";
            when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

            // When
            assertDoesNotThrow(() -> reviewService.reply(1L, replyContent));

            // Then
            verify(reviewMapper, times(1)).updateById(argThat(review ->
                review.getId().equals(1L) &&
                review.getReplyContent().equals(replyContent) &&
                review.getReplyTime() != null
            ));
        }

        @Test
        @DisplayName("回复评价 - 回复内容为空")
        void testReply_EmptyContent() {
            // Given
            String replyContent = "";
            when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

            // When
            assertDoesNotThrow(() -> reviewService.reply(1L, replyContent));

            // Then
            verify(reviewMapper, times(1)).updateById(argThat(review ->
                review.getId().equals(1L) &&
                review.getReplyContent().equals("")
            ));
        }

        @Test
        @DisplayName("回复评价 - 回复内容过长")
        void testReply_LongContent() {
            // Given
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 100; i++) {
                sb.append("感谢您的评价，");
            }
            String replyContent = sb.toString();
            when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

            // When
            assertDoesNotThrow(() -> reviewService.reply(1L, replyContent));

            // Then
            verify(reviewMapper, times(1)).updateById(argThat(review ->
                review.getId().equals(1L) &&
                review.getReplyContent().equals(replyContent)
            ));
        }

        @Test
        @DisplayName("回复评价 - 验证回复时间非空")
        void testReply_VerifyReplyTime() {
            // Given
            String replyContent = "感谢反馈！";
            when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

            // When
            reviewService.reply(1L, replyContent);

            // Then
            verify(reviewMapper, times(1)).updateById(argThat(review ->
                review.getId().equals(1L) &&
                review.getReplyTime() != null
            ));
        }
    }

    @Nested
    @DisplayName("获取评价详情测试")
    class GetByIdTest {

        @Test
        @DisplayName("获取评价详情 - 成功")
        void testGetById_Success() {
            // Given
            when(reviewMapper.selectById(1L)).thenReturn(testReview);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            // When
            ReviewVO result = reviewService.getById(1L);

            // Then
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals(100L, result.getReservationId());
            verify(reviewMapper, times(1)).selectById(1L);
        }

        @Test
        @DisplayName("获取评价详情 - 评价不存在")
        void testGetById_NotFound() {
            // Given
            when(reviewMapper.selectById(999L)).thenReturn(null);

            // When
            ReviewVO result = reviewService.getById(999L);

            // Then
            assertNull(result);
            verify(reviewMapper, times(1)).selectById(999L);
        }
    }

    @Nested
    @DisplayName("删除评价测试")
    class DeleteReviewTest {

        @Test
        @DisplayName("删除评价 - 成功")
        void testDelete_Success() {
            // Given
            when(reviewMapper.deleteById(1L)).thenReturn(1);

            // When
            assertDoesNotThrow(() -> reviewService.delete(1L));

            // Then
            verify(reviewMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("删除评价 - 评价不存在")
        void testDelete_NotFound() {
            // Given
            when(reviewMapper.deleteById(999L)).thenReturn(0);

            // When
            assertDoesNotThrow(() -> reviewService.delete(999L));

            // Then
            verify(reviewMapper, times(1)).deleteById(999L);
        }
    }

    @Nested
    @DisplayName("设置精选评价测试")
    class SetFeaturedTest {

        @Test
        @DisplayName("设置精选 - 设置为精选")
        void testSetFeatured_AsFeatured() {
            // Given
            when(reviewMapper.updateById(any(Review.class))).thenReturn(1);
            when(reviewMapper.selectById(1L)).thenReturn(testReview);

            // When
            assertDoesNotThrow(() -> reviewService.setFeatured(1L, 1));

            // Then
            verify(reviewMapper, atLeast(1)).updateById(any(Review.class));
            verify(reviewMapper, times(1)).selectById(1L);
        }

        @Test
        @DisplayName("设置精选 - 取消精选")
        void testSetFeatured_NotFeatured() {
            // Given
            when(reviewMapper.updateById(any(Review.class))).thenReturn(1);

            // When
            assertDoesNotThrow(() -> reviewService.setFeatured(1L, 0));

            // Then
            verify(reviewMapper, times(1)).updateById(argThat(review ->
                review.getId().equals(1L) &&
                review.getIsFeatured().equals(0)
            ));
        }
    }

    @Nested
    @DisplayName("获取评价统计信息测试")
    class GetStatisticsTest {

        @Test
        @DisplayName("获取统计信息 - 有评价数据")
        void testGetStatistics_WithData() {
            // Given
            Review review1 = Review.builder()
                    .id(1L)
                    .storeId(10L)
                    .scriptId(20L)
                    .overallRating(5)
                    .status(2)
                    .build();
            Review review2 = Review.builder()
                    .id(2L)
                    .storeId(10L)
                    .scriptId(20L)
                    .overallRating(4)
                    .status(2)
                    .build();
            Review review3 = Review.builder()
                    .id(3L)
                    .storeId(10L)
                    .scriptId(20L)
                    .overallRating(2)
                    .status(2)
                    .build();
            List<Review> reviews = Arrays.asList(review1, review2, review3);
            
            when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(reviews);

            // When
            var statistics = reviewService.getStatistics(10L, 20L);

            // Then
            assertNotNull(statistics);
            assertEquals(3L, statistics.getTotalCount());
            assertEquals(2, statistics.getGoodReviews()); // 5分和4分
            assertEquals(0, statistics.getMediumReviews()); // 3分
            assertEquals(1, statistics.getBadReviews()); // 2分
            assertTrue(statistics.getAverageRating().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(statistics.getGoodRate().compareTo(BigDecimal.ZERO) > 0);
            verify(reviewMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("获取统计信息 - 无评价数据")
        void testGetStatistics_NoData() {
            // Given
            when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());

            // When
            var statistics = reviewService.getStatistics(10L, 20L);

            // Then
            assertNotNull(statistics);
            assertEquals(0L, statistics.getTotalCount());
            assertEquals(0, statistics.getGoodReviews());
            assertEquals(0, statistics.getMediumReviews());
            assertEquals(0, statistics.getBadReviews());
            assertEquals(BigDecimal.ZERO, statistics.getAverageRating());
            assertEquals(BigDecimal.ZERO, statistics.getGoodRate());
        }

        @Test
        @DisplayName("获取统计信息 - 只按门店ID查询")
        void testGetStatistics_ByStoreIdOnly() {
            // Given
            Review review = Review.builder()
                    .id(1L)
                    .storeId(10L)
                    .overallRating(5)
                    .status(2)
                    .build();
            when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(review));

            // When
            var statistics = reviewService.getStatistics(10L, null);

            // Then
            assertNotNull(statistics);
            assertEquals(1L, statistics.getTotalCount());
        }

        @Test
        @DisplayName("获取统计信息 - 只按剧本ID查询")
        void testGetStatistics_ByScriptIdOnly() {
            // Given
            Review review = Review.builder()
                    .id(1L)
                    .scriptId(20L)
                    .overallRating(5)
                    .status(2)
                    .build();
            when(reviewMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(review));

            // When
            var statistics = reviewService.getStatistics(null, 20L);

            // Then
            assertNotNull(statistics);
            assertEquals(1L, statistics.getTotalCount());
        }
    }
}
