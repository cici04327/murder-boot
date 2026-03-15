package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ScriptReviewDTO;
import com.murder.entity.Script;
import com.murder.entity.ScriptReview;
import com.murder.entity.User;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.ScriptReviewMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.impl.ScriptReviewServiceImpl;
import com.murder.vo.ScriptReviewVO;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 剧本评价服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("剧本评价服务测试")
class ScriptReviewServiceTest {

    @Mock
    private ScriptReviewMapper reviewMapper;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private ScriptReviewServiceImpl scriptReviewService;

    private ScriptReview testReview;
    private ScriptReviewDTO testDTO;
    private Script testScript;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setNickname("测试用户");

        testScript = Script.builder()
                .id(1L)
                .name("测试剧本")
                .rating(new BigDecimal("4.5"))
                .build();

        testReview = new ScriptReview();
        testReview.setId(1L);
        testReview.setScriptId(1L);
        testReview.setUserId(1L);
        testReview.setReservationId(100L);
        testReview.setRating(5);
        testReview.setContent("剧本非常精彩！");
        testReview.setCreateTime(LocalDateTime.now());

        testDTO = new ScriptReviewDTO();
        testDTO.setScriptId(1L);
        testDTO.setRating(5);
        testDTO.setContent("剧本非常精彩！");
    }

    @Nested
    @DisplayName("添加剧本评价测试")
    class AddTests {

        @Test
        @DisplayName("添加评价 - 成功")
        void testAdd_Success() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(1L);
                when(reviewMapper.insert(any(ScriptReview.class))).thenReturn(1);
                when(reviewMapper.calculateAverageRating(1L)).thenReturn(new BigDecimal("4.5"));
                when(scriptMapper.updateById(any(Script.class))).thenReturn(1);
                ArgumentCaptor<ScriptReview> reviewCaptor = ArgumentCaptor.forClass(ScriptReview.class);
                ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);

                scriptReviewService.add(testDTO);
                verify(reviewMapper, times(1)).insert(reviewCaptor.capture());
                verify(scriptMapper, times(1)).updateById(scriptCaptor.capture());

                ScriptReview created = reviewCaptor.getValue();
                assertEquals(1L, created.getScriptId());
                assertEquals(1L, created.getUserId());
                assertEquals(5, created.getRating());
                assertEquals("剧本非常精彩！", created.getContent());

                Script updatedScript = scriptCaptor.getValue();
                assertEquals(1L, updatedScript.getId());
                assertEquals(new BigDecimal("4.50"), updatedScript.getRating());
            }
        }

        @Test
        @DisplayName("添加评价 - 未登录用户也可评价（userId为null）")
        void testAdd_AnonymousUser() {
            try (MockedStatic<BaseContext> mocked = mockStatic(BaseContext.class)) {
                mocked.when(BaseContext::getCurrentId).thenReturn(null);
                when(reviewMapper.insert(any(ScriptReview.class))).thenReturn(1);
                when(reviewMapper.calculateAverageRating(1L)).thenReturn(new BigDecimal("4.5"));
                when(scriptMapper.updateById(any(Script.class))).thenReturn(1);
                ArgumentCaptor<ScriptReview> reviewCaptor = ArgumentCaptor.forClass(ScriptReview.class);

                scriptReviewService.add(testDTO);
                verify(reviewMapper, times(1)).insert(reviewCaptor.capture());
                assertNull(reviewCaptor.getValue().getUserId());
            }
        }
    }

    @Nested
    @DisplayName("分页查询剧本评价测试")
    class PageQueryTests {

        @Test
        @DisplayName("分页查询 - 按剧本ID查询")
        void testPageQuery_ByScriptId() {
            List<ScriptReview> reviews = Collections.singletonList(testReview);
            when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<ScriptReview> p = inv.getArgument(0);
                        p.setRecords(reviews);
                        return p;
                    });
            when(scriptMapper.selectById(1L)).thenReturn(testScript);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            PageResult<ScriptReviewVO> result = scriptReviewService.pageQuery(1L, 1, 10);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertFalse(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("分页查询 - 无数据")
        void testPageQuery_Empty() {
            when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenAnswer(inv -> {
                        Page<ScriptReview> p = inv.getArgument(0);
                        p.setRecords(Collections.emptyList());
                        return p;
                    });

            PageResult<ScriptReviewVO> result = scriptReviewService.pageQuery(1L, 1, 10);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("查询评价详情测试")
    class GetByIdTests {

        @Test
        @DisplayName("按ID查询 - 存在")
        void testGetById_Found() {
            when(reviewMapper.selectById(1L)).thenReturn(testReview);
            when(scriptMapper.selectById(1L)).thenReturn(testScript);
            when(userMapper.selectById(1L)).thenReturn(testUser);

            ScriptReviewVO result = scriptReviewService.getById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("按ID查询 - 不存在返回null")
        void testGetById_NotFound() {
            when(reviewMapper.selectById(999L)).thenReturn(null);

            ScriptReviewVO result = scriptReviewService.getById(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("删除评价测试")
    class DeleteTests {

        @Test
        @DisplayName("删除评价 - 成功并更新剧本评分")
        void testDelete_Success() {
            when(reviewMapper.selectById(1L)).thenReturn(testReview);
            when(reviewMapper.deleteById(1L)).thenReturn(1);
            when(reviewMapper.calculateAverageRating(1L)).thenReturn(new BigDecimal("4.0"));
            when(scriptMapper.updateById(any(Script.class))).thenReturn(1);
            ArgumentCaptor<Script> scriptCaptor = ArgumentCaptor.forClass(Script.class);

            scriptReviewService.delete(1L);
            verify(reviewMapper, times(1)).deleteById(1L);
            verify(scriptMapper, times(1)).updateById(scriptCaptor.capture());

            Script updatedScript = scriptCaptor.getValue();
            assertEquals(1L, updatedScript.getId());
            assertEquals(new BigDecimal("4.00"), updatedScript.getRating());
        }

        @Test
        @DisplayName("删除评价 - 不存在则不操作")
        void testDelete_NotFound() {
            when(reviewMapper.selectById(999L)).thenReturn(null);

            scriptReviewService.delete(999L);
            verify(reviewMapper, never()).deleteById(anyLong());
            verify(scriptMapper, never()).updateById(any(Script.class));
        }
    }

    @Nested
    @DisplayName("按预约ID查询评价测试")
    class GetByReservationIdTests {

        @Test
        @DisplayName("按预约ID查询 - 存在")
        void testGetByReservationId_Found() {
            when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testReview);

            ScriptReview result = scriptReviewService.getByReservationId(100L);

            assertNotNull(result);
            assertEquals(100L, result.getReservationId());
        }

        @Test
        @DisplayName("按预约ID查询 - 不存在")
        void testGetByReservationId_NotFound() {
            when(reviewMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            ScriptReview result = scriptReviewService.getByReservationId(999L);

            assertNull(result);
        }
    }
}
