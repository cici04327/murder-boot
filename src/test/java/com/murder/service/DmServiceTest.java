package com.murder.service;

import com.murder.dto.DmDTO;
import com.murder.entity.Dm;
import com.murder.entity.Review;
import com.murder.entity.Store;
import com.murder.mapper.DmMapper;
import com.murder.mapper.ReviewMapper;
import com.murder.mapper.StoreMapper;
import com.murder.service.impl.DmServiceImpl;
import com.murder.vo.DmVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DM服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DM服务测试")
class DmServiceTest {

    @Mock
    private DmMapper dmMapper;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private DmServiceImpl dmService;

    private Dm testDm;
    private Store testStore;
    private DmDTO testDmDTO;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testStore = Store.builder()
                .id(1L)
                .name("测试门店")
                .build();

        testDm = Dm.builder()
                .id(1L)
                .storeId(1L)
                .name("张三")
                .avatar("http://example.com/avatar1.jpg")
                .introduction("资深DM")
                .styleTags("推理型,情感型")
                .rating(new BigDecimal("4.5"))
                .gameCount(50)
                .status(1)
                .createTime(LocalDateTime.now())
                .build();

        testDmDTO = new DmDTO();
        testDmDTO.setId(1L);
        testDmDTO.setStoreId(1L);
        testDmDTO.setName("张三");
        testDmDTO.setAvatar("http://example.com/avatar1.jpg");
        testDmDTO.setIntroduction("资深DM");
        testDmDTO.setStyleTags("推理型,情感型");
        testDmDTO.setStatus(1);
    }

    @Nested
    @DisplayName("查询操作")
    class QueryTests {

        @Test
        @DisplayName("listByStoreId - 按门店查询DM列表（正常情况）")
        void testListByStoreId_Success() {
            // Given
            List<Dm> dmList = Arrays.asList(testDm);
            when(dmMapper.selectList(any())).thenReturn(dmList);
            when(storeMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testStore));

            // When
            List<DmVO> result = dmService.listByStoreId(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("张三", result.get(0).getName());
            assertEquals("测试门店", result.get(0).getStoreName());
            verify(dmMapper, times(1)).selectList(any());
            verify(storeMapper, times(1)).selectBatchIds(anyList());
        }

        @Test
        @DisplayName("listByStoreId - 按门店查询DM列表（空列表）")
        void testListByStoreId_Empty() {
            // Given
            when(dmMapper.selectList(any())).thenReturn(new ArrayList<>());

            // When
            List<DmVO> result = dmService.listByStoreId(1L);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(dmMapper, times(1)).selectList(any());
        }

        @Test
        @DisplayName("getById - 查询单个DM（正常情况）")
        void testGetById_Success() {
            // Given
            when(dmMapper.selectById(1L)).thenReturn(testDm);
            when(storeMapper.selectBatchIds(anyList())).thenReturn(Collections.singletonList(testStore));

            // When
            DmVO result = dmService.getById(1L);

            // Then
            assertNotNull(result);
            assertEquals("张三", result.getName());
            assertEquals("测试门店", result.getStoreName());
            verify(dmMapper, times(1)).selectById(1L);
        }

        @Test
        @DisplayName("getById - 查询单个DM（不存在）")
        void testGetById_NotFound() {
            // Given
            when(dmMapper.selectById(999L)).thenReturn(null);

            // When
            DmVO result = dmService.getById(999L);

            // Then
            assertNull(result);
            verify(dmMapper, times(1)).selectById(999L);
        }
    }

    @Nested
    @DisplayName("新增操作")
    class AddTests {

        @Test
        @DisplayName("add - 新增DM（正常情况）")
        void testAdd_Success() {
            // Given
            when(dmMapper.insert(any(Dm.class))).thenReturn(1);

            // When
            dmService.add(testDmDTO);

            // Then
            verify(dmMapper, times(1)).insert(any(Dm.class));
        }

        @Test
        @DisplayName("add - 新增DM时初始化默认值")
        void testAdd_InitialValues() {
            // Given
            when(dmMapper.insert(any(Dm.class))).thenAnswer(invocation -> {
                Dm dm = invocation.getArgument(0);
                assertNull(dm.getId());
                assertEquals(BigDecimal.ZERO, dm.getRating());
                assertEquals(0, dm.getGameCount());
                assertEquals(1, dm.getStatus());
                return 1;
            });

            // When
            dmService.add(testDmDTO);

            // Then
            verify(dmMapper, times(1)).insert(any(Dm.class));
        }
    }

    @Nested
    @DisplayName("更新操作")
    class UpdateTests {

        @Test
        @DisplayName("update - 更新DM（正常情况）")
        void testUpdate_Success() {
            // Given
            when(dmMapper.updateById(any(Dm.class))).thenReturn(1);

            // When
            dmService.update(testDmDTO);

            // Then
            verify(dmMapper, times(1)).updateById(any(Dm.class));
        }

        @Test
        @DisplayName("update - 更新DM时不修改评分和场次")
        void testUpdate_RatingAndGameCountUnchanged() {
            // Given
            when(dmMapper.updateById(any(Dm.class))).thenAnswer(invocation -> {
                Dm dm = invocation.getArgument(0);
                assertNull(dm.getRating());
                assertNull(dm.getGameCount());
                return 1;
            });

            // When
            dmService.update(testDmDTO);

            // Then
            verify(dmMapper, times(1)).updateById(any(Dm.class));
        }
    }

    @Nested
    @DisplayName("删除操作")
    class DeleteTests {

        @Test
        @DisplayName("delete - 删除DM（正常情况）")
        void testDelete_Success() {
            // Given
            when(dmMapper.deleteById(1L)).thenReturn(1);

            // When
            dmService.delete(1L);

            // Then
            verify(dmMapper, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("评分刷新操作")
    class RatingTests {

        @Test
        @DisplayName("refreshRating - 更新评分（正常情况）")
        void testRefreshRating_Success() {
            // Given
            List<Review> reviews = Arrays.asList(
                    Review.builder().id(1L).dmId(1L).dmRating(5).status(2).build(),
                    Review.builder().id(2L).dmId(1L).dmRating(4).status(2).build(),
                    Review.builder().id(3L).dmId(1L).dmRating(4).status(2).build()
            );
            when(reviewMapper.selectList(any())).thenReturn(reviews);

            // When
            dmService.refreshRating(1L);

            // Then
            verify(reviewMapper, times(1)).selectList(any());
            verify(dmMapper, times(1)).updateRatingAndGameCount(eq(1L), any(BigDecimal.class), eq(3));
        }

        @Test
        @DisplayName("refreshRating - 无评价时不更新")
        void testRefreshRating_NoReviews() {
            // Given
            when(reviewMapper.selectList(any())).thenReturn(new ArrayList<>());

            // When
            dmService.refreshRating(1L);

            // Then
            verify(reviewMapper, times(1)).selectList(any());
            verify(dmMapper, never()).updateRatingAndGameCount(anyLong(), any(BigDecimal.class), anyInt());
        }

        @Test
        @DisplayName("refreshRating - 评分正确计算")
        void testRefreshRating_CalculationAccuracy() {
            // Given
            List<Review> reviews = Arrays.asList(
                    Review.builder().id(1L).dmId(1L).dmRating(3).status(2).build(),
                    Review.builder().id(2L).dmId(1L).dmRating(4).status(2).build()
            );
            when(reviewMapper.selectList(any())).thenReturn(reviews);

            // When
            dmService.refreshRating(1L);

            // Then
            verify(dmMapper, times(1)).updateRatingAndGameCount(
                    eq(1L),
                    argThat(rating -> rating.compareTo(new BigDecimal("3.5")) == 0),
                    eq(2)
            );
        }
    }
}
