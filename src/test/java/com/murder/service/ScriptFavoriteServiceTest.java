package com.murder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.entity.Script;
import com.murder.entity.ScriptFavorite;
import com.murder.mapper.ScriptFavoriteMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.service.impl.ScriptFavoriteServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 剧本收藏服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("剧本收藏服务测试")
class ScriptFavoriteServiceTest {

    @Mock
    private ScriptFavoriteMapper scriptFavoriteMapper;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private UserPointsService userPointsService;

    @InjectMocks
    private ScriptFavoriteServiceImpl scriptFavoriteService;

    private Script testScript;
    private ScriptFavorite testFavorite;

    @BeforeEach
    void setUp() {
        testScript = Script.builder()
                .id(1L)
                .name("测试剧本")
                .categoryId(1L)
                .price(new BigDecimal("198.00"))
                .status(1)
                .build();

        testFavorite = new ScriptFavorite();
        testFavorite.setId(1L);
        testFavorite.setUserId(1L);
        testFavorite.setScriptId(1L);
        testFavorite.setCreateTime(LocalDateTime.now());
        testFavorite.setUpdateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("收藏剧本测试")
    class FavoriteScriptTests {

        @Test
        @DisplayName("收藏剧本 - 成功")
        void testFavoriteScript_Success() {
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(0L);
            when(scriptFavoriteMapper.insert(any(ScriptFavorite.class))).thenReturn(1);
            // 模拟getFavoriteCount: 第一次调用0，insert后第二次调用1
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(0L, 1L);

            assertDoesNotThrow(() -> scriptFavoriteService.favoriteScript(1L, 1L));
            verify(scriptFavoriteMapper, times(1)).insert(any(ScriptFavorite.class));
        }

        @Test
        @DisplayName("收藏剧本 - 已收藏则抛出异常")
        void testFavoriteScript_AlreadyFavorited() {
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(1L);

            assertThrows(RuntimeException.class, () -> scriptFavoriteService.favoriteScript(1L, 1L));
            verify(scriptFavoriteMapper, never()).insert(any());
        }

        @Test
        @DisplayName("收藏剧本 - 每收藏5个获得积分奖励")
        void testFavoriteScript_PointsRewardAtMultipleOfFive() {
            // 先返回0（第一次check），insert后返回5（触发积分奖励）
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(0L, 5L);
            when(scriptFavoriteMapper.insert(any(ScriptFavorite.class))).thenReturn(1);
            doNothing().when(userPointsService).addPoints(anyLong(), anyInt(), anyString());

            assertDoesNotThrow(() -> scriptFavoriteService.favoriteScript(1L, 1L));
            verify(userPointsService, times(1)).addPoints(eq(1L), eq(20), anyString());
        }
    }

    @Nested
    @DisplayName("取消收藏测试")
    class UnfavoriteScriptTests {

        @Test
        @DisplayName("取消收藏 - 成功")
        void testUnfavoriteScript_Success() {
            when(scriptFavoriteMapper.delete(any())).thenReturn(1);

            assertDoesNotThrow(() -> scriptFavoriteService.unfavoriteScript(1L, 1L));
            verify(scriptFavoriteMapper, times(1)).delete(any());
        }
    }

    @Nested
    @DisplayName("检查收藏状态测试")
    class IsFavoritedTests {

        @Test
        @DisplayName("已收藏 - 返回true")
        void testIsFavorited_True() {
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(1L);

            boolean result = scriptFavoriteService.isFavorited(1L, 1L);

            assertTrue(result);
        }

        @Test
        @DisplayName("未收藏 - 返回false")
        void testIsFavorited_False() {
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(0L);

            boolean result = scriptFavoriteService.isFavorited(1L, 1L);

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("获取收藏列表测试")
    class GetUserFavoritesTests {

        @Test
        @DisplayName("获取收藏列表 - 有数据")
        void testGetUserFavorites_WithData() {
            List<ScriptFavorite> favorites = Collections.singletonList(testFavorite);
            when(scriptFavoriteMapper.selectPage(any(Page.class), any()))
                    .thenAnswer(inv -> {
                        Page<ScriptFavorite> p = inv.getArgument(0);
                        p.setRecords(favorites);
                        p.setTotal(1);
                        return p;
                    });
            when(scriptMapper.selectList(any())).thenReturn(Collections.singletonList(testScript));

            PageResult<Script> result = scriptFavoriteService.getUserFavorites(1L, 1, 10);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
            assertFalse(result.getRecords().isEmpty());
        }

        @Test
        @DisplayName("获取收藏列表 - 无数据")
        void testGetUserFavorites_Empty() {
            when(scriptFavoriteMapper.selectPage(any(Page.class), any()))
                    .thenAnswer(inv -> {
                        Page<ScriptFavorite> p = inv.getArgument(0);
                        p.setRecords(Collections.emptyList());
                        p.setTotal(0);
                        return p;
                    });

            PageResult<Script> result = scriptFavoriteService.getUserFavorites(1L, 1, 10);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("获取收藏数量测试")
    class GetFavoriteCountTests {

        @Test
        @DisplayName("获取收藏数量 - 有收藏")
        void testGetFavoriteCount_WithFavorites() {
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(3L);

            Integer count = scriptFavoriteService.getFavoriteCount(1L);

            assertEquals(3, count);
        }

        @Test
        @DisplayName("获取收藏数量 - 无收藏")
        void testGetFavoriteCount_NoFavorites() {
            when(scriptFavoriteMapper.selectCount(any())).thenReturn(0L);

            Integer count = scriptFavoriteService.getFavoriteCount(1L);

            assertEquals(0, count);
        }
    }
}
