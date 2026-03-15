package com.murder.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.entity.Script;
import com.murder.entity.UserBrowseHistory;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.UserBrowseHistoryMapper;
import com.murder.service.impl.UserBrowseHistoryServiceImpl;
import com.murder.vo.BrowseHistoryVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户浏览历史服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户浏览历史服务测试")
class UserBrowseHistoryServiceTest {

    @Mock
    private UserBrowseHistoryMapper browseHistoryMapper;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private StoreMapper storeMapper;

    @InjectMocks
    private UserBrowseHistoryServiceImpl browseHistoryService;

    private UserBrowseHistory testHistory;

    @BeforeEach
    void setUp() {
        if (TableInfoHelper.getTableInfo(UserBrowseHistory.class) == null) {
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), UserBrowseHistory.class);
        }
        ReflectionTestUtils.setField(browseHistoryService, "baseMapper", browseHistoryMapper);
        ReflectionTestUtils.setField(browseHistoryService, "entityClass", UserBrowseHistory.class);
        ReflectionTestUtils.setField(browseHistoryService, "mapperClass", UserBrowseHistoryMapper.class);

        testHistory = new UserBrowseHistory();
        testHistory.setId(1L);
        testHistory.setUserId(1L);
        testHistory.setTargetType(1); // 1=剧本
        testHistory.setTargetId(10L);
        testHistory.setBrowseTime(LocalDateTime.now());
        testHistory.setDuration(120);
        testHistory.setCreateTime(LocalDateTime.now());
    }

    @Nested
    @DisplayName("分页查询历史测试")
    class PageHistoryTests {

        @Test
        @DisplayName("分页查询 - 有数据")
        void testPageHistory_WithData() {
            when(browseHistoryMapper.selectPage(any(Page.class), any()))
                    .thenAnswer(inv -> {
                        Page<UserBrowseHistory> p = inv.getArgument(0);
                        p.setRecords(Collections.singletonList(testHistory));
                        p.setTotal(1);
                        return p;
                    });
            when(scriptMapper.selectBatchIds(anyCollection())).thenReturn(
                    Collections.singletonList(Script.builder().id(10L).name("测试剧本").build())
            );

            Page<BrowseHistoryVO> result = browseHistoryService.pageHistory(1L, 1, 10, null, null);

            assertNotNull(result);
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("分页查询 - 按类型过滤")
        void testPageHistory_ByTargetType() {
            when(browseHistoryMapper.selectPage(any(Page.class), any()))
                    .thenAnswer(inv -> {
                        Page<UserBrowseHistory> p = inv.getArgument(0);
                        p.setRecords(Collections.singletonList(testHistory));
                        p.setTotal(1);
                        return p;
                    });
            when(scriptMapper.selectBatchIds(anyCollection())).thenReturn(
                    Collections.singletonList(Script.builder().id(10L).name("测试剧本").build())
            );

            Page<BrowseHistoryVO> result = browseHistoryService.pageHistory(1L, 1, 10, 30, 1);

            assertNotNull(result);
        }

        @Test
        @DisplayName("分页查询 - 无数据")
        void testPageHistory_Empty() {
            when(browseHistoryMapper.selectPage(any(Page.class), any()))
                    .thenAnswer(inv -> {
                        Page<UserBrowseHistory> p = inv.getArgument(0);
                        p.setRecords(Collections.emptyList());
                        p.setTotal(0);
                        return p;
                    });

            Page<BrowseHistoryVO> result = browseHistoryService.pageHistory(1L, 1, 10, null, null);

            assertNotNull(result);
            assertEquals(0L, result.getTotal());
            assertTrue(result.getRecords().isEmpty());
        }
    }

    @Nested
    @DisplayName("记录浏览历史测试")
    class RecordHistoryTests {

        @Test
        @DisplayName("记录浏览 - 新记录")
        void testRecordHistory_NewRecord() {
            when(browseHistoryMapper.selectOne(any())).thenReturn(null);
            when(browseHistoryMapper.insert(any(UserBrowseHistory.class))).thenReturn(1);

            assertDoesNotThrow(() -> browseHistoryService.recordHistory(1L, 1, 10L, 120));
            verify(browseHistoryMapper, times(1)).insert(any(UserBrowseHistory.class));
        }

        @Test
        @DisplayName("记录浏览 - 已有记录则更新")
        void testRecordHistory_UpdateExisting() {
            when(browseHistoryMapper.selectOne(any())).thenReturn(testHistory);
            when(browseHistoryMapper.updateById(any(UserBrowseHistory.class))).thenReturn(1);

            assertDoesNotThrow(() -> browseHistoryService.recordHistory(1L, 1, 10L, 60));
            verify(browseHistoryMapper, times(1)).updateById(any(UserBrowseHistory.class));
            verify(browseHistoryMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("删除浏览历史测试")
    class DeleteHistoryTests {

        @Test
        @DisplayName("删除单条记录 - 成功")
        void testDeleteHistory_Success() {
            when(browseHistoryMapper.update(isNull(), any())).thenReturn(1);

            boolean result = browseHistoryService.deleteHistory(1L, 1L);

            assertTrue(result);
            verify(browseHistoryMapper, times(1)).update(isNull(), any());
        }

        @Test
        @DisplayName("删除单条记录 - 记录不属于该用户")
        void testDeleteHistory_NotOwned() {
            when(browseHistoryMapper.update(isNull(), any())).thenReturn(0);

            boolean result = browseHistoryService.deleteHistory(1L, 999L);

            assertFalse(result);
            verify(browseHistoryMapper, times(1)).update(isNull(), any());
        }

        @Test
        @DisplayName("清空用户历史 - 成功")
        void testClearHistory_Success() {
            when(browseHistoryMapper.delete(any())).thenReturn(5);

            int count = browseHistoryService.clearHistory(1L);

            assertEquals(5, count);
            verify(browseHistoryMapper, times(1)).delete(any());
        }
    }

    @Nested
    @DisplayName("统计测试")
    class StatsTests {

        @Test
        @DisplayName("获取浏览历史统计 - 成功")
        void testGetHistoryStats_Success() {
            when(browseHistoryMapper.selectCount(any())).thenReturn(10L, 7L, 3L, 2L, 5L);

            UserBrowseHistoryService.BrowseHistoryStatsVO stats =
                    browseHistoryService.getHistoryStats(1L);

            assertNotNull(stats);
            assertNotNull(stats.getTotalCount());
        }
    }
}
