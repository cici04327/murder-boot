package com.murder.service;

import com.murder.mapper.*;
import com.murder.service.impl.StatisticsServiceImpl;
import com.murder.vo.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 统计服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("统计服务测试")
class StatisticsServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private ReviewMapper reviewMapper;

    @Mock
    private UserPointsRecordMapper userPointsRecordMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private StatisticsServiceImpl statisticsService;

    @BeforeEach
    void setUpJdbcTemplate() {
        lenient().doReturn(BigDecimal.ZERO)
                .when(jdbcTemplate).queryForObject(anyString(), eq(BigDecimal.class), any(Object[].class));
        lenient().doReturn(BigDecimal.ZERO)
                .when(jdbcTemplate).queryForObject(anyString(), eq(BigDecimal.class));
        lenient().doReturn(0)
                .when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), any(Object[].class));
        lenient().doReturn(0)
                .when(jdbcTemplate).queryForObject(anyString(), eq(Integer.class));
        lenient().doReturn(Collections.emptyList())
                .when(jdbcTemplate).queryForList(anyString(), any(Object[].class));
        lenient().doReturn(Collections.emptyList())
                .when(jdbcTemplate).queryForList(anyString());
    }

    @Nested
    @DisplayName("概览统计测试")
    class OverviewTests {

        @Test
        @DisplayName("获取概览数据 - 成功")
        void testGetOverview_Success() {
            when(userMapper.selectCount(any())).thenReturn(1000L);
            when(reservationMapper.selectCount(any())).thenReturn(500L);
            when(storeMapper.selectCount(any())).thenReturn(20L);
            when(scriptMapper.selectCount(any())).thenReturn(150L);
            when(reviewMapper.selectCount(any())).thenReturn(300L);
            // 模拟今日/本月数据
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsOverviewVO result = statisticsService.getOverview();

            assertNotNull(result);
            // 验证基础字段有值
            assertNotNull(result.getTotalUsers());
            assertNotNull(result.getTodayReservations());
        }

        @Test
        @DisplayName("获取概览数据 - 数据为0时正常返回")
        void testGetOverview_ZeroData() {
            when(userMapper.selectCount(any())).thenReturn(0L);
            when(reservationMapper.selectCount(any())).thenReturn(0L);
            when(storeMapper.selectCount(any())).thenReturn(0L);
            when(scriptMapper.selectCount(any())).thenReturn(0L);
            when(reviewMapper.selectCount(any())).thenReturn(0L);
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsOverviewVO result = statisticsService.getOverview();

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("图表数据测试")
    class ChartsTests {

        @Test
        @DisplayName("获取图表数据 - 30天")
        void testGetCharts_30Days() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsChartsVO result = statisticsService.getCharts(30);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取图表数据 - 7天")
        void testGetCharts_7Days() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsChartsVO result = statisticsService.getCharts(7);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取图表数据 - days为null时默认30天")
        void testGetCharts_NullDays() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsChartsVO result = statisticsService.getCharts(null);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("排行榜测试")
    class RankingsTests {

        @Test
        @DisplayName("获取排行榜 - 成功")
        void testGetRankings_Success() {
            when(scriptMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(storeMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsRankingVO result = statisticsService.getRankings(10);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取排行榜 - limit为null时使用默认值")
        void testGetRankings_NullLimit() {
            when(scriptMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(storeMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsRankingVO result = statisticsService.getRankings(null);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("实时数据测试")
    class RealtimeTests {

        @Test
        @DisplayName("获取实时数据 - 成功")
        void testGetRealtime_Success() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            StatisticsRealtimeVO result = statisticsService.getRealtime(20);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("经营看板测试")
    class OperationBoardTests {

        @Test
        @DisplayName("获取经营看板 - 全部门店")
        void testGetOperationBoard_AllStores() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectCount(any())).thenReturn(0L);
            when(userMapper.selectList(any())).thenReturn(Collections.emptyList());

            Map<String, Object> result = statisticsService.getOperationBoard(30, null);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取经营看板 - 指定门店")
        void testGetOperationBoard_ByStore() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectCount(any())).thenReturn(0L);

            Map<String, Object> result = statisticsService.getOperationBoard(30, 1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取经营看板 - days为null时默认30天")
        void testGetOperationBoard_NullDays() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectCount(any())).thenReturn(0L);

            Map<String, Object> result = statisticsService.getOperationBoard(null, null);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("门店日报测试")
    class StoreDailyReportTests {

        @Test
        @DisplayName("获取门店日报 - 指定门店")
        void testGetStoreDailyReport_ByStore() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectCount(any())).thenReturn(0L);

            Map<String, Object> result = statisticsService.getStoreDailyReport(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取门店日报 - 全部门店（storeId=null）")
        void testGetStoreDailyReport_AllStores() {
            when(reservationMapper.selectList(any())).thenReturn(Collections.emptyList());
            when(reservationMapper.selectCount(any())).thenReturn(0L);

            Map<String, Object> result = statisticsService.getStoreDailyReport(null);

            assertNotNull(result);
        }
    }
}
