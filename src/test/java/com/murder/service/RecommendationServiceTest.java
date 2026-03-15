package com.murder.service;

import com.murder.entity.HotRanking;
import com.murder.entity.RecommendationLog;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.UserBrowseHistory;
import com.murder.entity.UserPreference;
import com.murder.mapper.*;
import com.murder.service.impl.RecommendationServiceImpl;
import com.murder.vo.RecommendationVO;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 推荐服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("推荐服务测试")
class RecommendationServiceTest {

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private UserBrowseHistoryMapper browseHistoryMapper;

    @Mock
    private UserPreferenceMapper userPreferenceMapper;

    @Mock
    private RecommendationLogMapper recommendationLogMapper;

    @Mock
    private HotRankingMapper hotRankingMapper;

    @Mock
    private ScriptTagMapper scriptTagMapper;

    @Mock
    private ScriptFavoriteMapper scriptFavoriteMapper;

    @Mock
    private ReservationMapper reservationMapper;

    @Mock
    private ScriptCategoryMapper scriptCategoryMapper;

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private Script testScript;
    private HotRanking testRanking;
    private UserPreference testPreference;
    private ScriptCategory testCategory;

    @BeforeEach
    void setUp() {
        testScript = Script.builder()
                .id(1L)
                .name("热门剧本")
                .categoryId(1L)
                .type(1)
                .difficulty(2)
                .rating(new BigDecimal("4.8"))
                .price(new BigDecimal("198.00"))
                .playerCount(6)
                .status(1)
                .build();

        testRanking = new HotRanking();
        testRanking.setId(1L);
        testRanking.setScriptId(1L);
        testRanking.setRankingType(1);
        testRanking.setScore(new BigDecimal("95.5"));
        testRanking.setRank(1);
        testRanking.setUpdateTime(LocalDateTime.now());

        testPreference = new UserPreference();
        testPreference.setId(1L);
        testPreference.setUserId(1L);
        testPreference.setPreferenceType("category");
        testPreference.setPreferenceValue("1");
        testPreference.setScore(new BigDecimal("0.8"));
        testPreference.setUpdateTime(LocalDateTime.now());

        testCategory = new ScriptCategory();
        testCategory.setId(1L);
        testCategory.setName("推理");

        lenient().when(scriptTagMapper.getScriptTags(anyLong())).thenReturn(Collections.singletonList("烧脑"));
        lenient().when(scriptTagMapper.batchGetScriptTags(anyList()))
                .thenReturn(Collections.singletonMap(1L, Collections.singletonList("烧脑")));
        lenient().when(scriptTagMapper.getSimilarScriptIdsByTags(anyList(), anyLong(), anyInt()))
                .thenReturn(Collections.emptyList());
        lenient().when(scriptFavoriteMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(scriptCategoryMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(testCategory));
        lenient().when(hotRankingMapper.getHotRankingList(anyInt(), anyInt()))
                .thenReturn(Collections.singletonList(testRanking));
        lenient().when(scriptMapper.selectBatchIds(anyCollection()))
                .thenReturn(Collections.singletonList(testScript));
        lenient().when(scriptMapper.selectById(1L)).thenReturn(testScript);
    }

    @Nested
    @DisplayName("个性化推荐测试")
    class PersonalizedRecommendationTests {

        @Test
        @DisplayName("获取个性化推荐 - 登录用户有偏好数据")
        void testGetPersonalizedRecommendations_WithPreferences() {
            when(userPreferenceMapper.getUserTopPreferences(eq(1L), anyInt())).thenReturn(
                    Collections.singletonList(testPreference));
            when(scriptMapper.selectList(any())).thenReturn(Collections.singletonList(testScript));

            List<RecommendationVO> result =
                    recommendationService.getPersonalizedRecommendations(1L, 10);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取个性化推荐 - 无偏好数据降级为热门")
        void testGetPersonalizedRecommendations_FallbackToHot() {
            when(userPreferenceMapper.getUserTopPreferences(eq(1L), anyInt())).thenReturn(Collections.emptyList());

            List<RecommendationVO> result =
                    recommendationService.getPersonalizedRecommendations(1L, 10);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取个性化推荐 - 匿名用户返回热门")
        void testGetPersonalizedRecommendations_AnonymousUser() {
            List<RecommendationVO> result =
                    recommendationService.getPersonalizedRecommendations(null, 10);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("热门推荐测试")
    class HotRecommendationTests {

        @Test
        @DisplayName("获取今日热门 - 有数据")
        void testGetHotRecommendations_Today() {
            List<RecommendationVO> result =
                    recommendationService.getHotRecommendations(1, 10);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取本周热门 - 有数据")
        void testGetHotRecommendations_ThisWeek() {
            List<RecommendationVO> result =
                    recommendationService.getHotRecommendations(2, 10);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取热门推荐 - 无数据")
        void testGetHotRecommendations_Empty() {
            when(hotRankingMapper.getHotRankingList(anyInt(), anyInt())).thenReturn(Collections.emptyList());
            when(scriptMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<RecommendationVO> result =
                    recommendationService.getHotRecommendations(1, 10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("新品推荐测试")
    class NewScriptRecommendationTests {

        @Test
        @DisplayName("获取新品推荐 - 有数据")
        void testGetNewScriptRecommendations_WithData() {
            when(scriptMapper.selectList(any())).thenReturn(
                    Collections.singletonList(testScript));

            List<RecommendationVO> result =
                    recommendationService.getNewScriptRecommendations(10);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取新品推荐 - 无数据")
        void testGetNewScriptRecommendations_Empty() {
            when(scriptMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<RecommendationVO> result =
                    recommendationService.getNewScriptRecommendations(10);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("基于内容的推荐测试")
    class ContentBasedRecommendationTests {

        @Test
        @DisplayName("获取相似剧本推荐 - 有数据")
        void testGetContentBasedRecommendations_WithData() {
            when(scriptMapper.selectById(1L)).thenReturn(testScript);
            when(scriptMapper.selectList(any())).thenReturn(
                    Arrays.asList(testScript));

            List<RecommendationVO> result =
                    recommendationService.getContentBasedRecommendations(1L, 5);

            assertNotNull(result);
        }

        @Test
        @DisplayName("获取相似剧本推荐 - 剧本不存在")
        void testGetContentBasedRecommendations_ScriptNotFound() {
            when(scriptMapper.selectById(999L)).thenReturn(null);

            List<RecommendationVO> result =
                    recommendationService.getContentBasedRecommendations(999L, 5);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("行为记录测试")
    class BehaviorRecordTests {

        @Test
        @DisplayName("记录浏览行为")
        void testRecordBrowseHistory() {
            when(browseHistoryMapper.selectOne(any())).thenReturn(null);
            when(browseHistoryMapper.insert(any(UserBrowseHistory.class))).thenReturn(1);
            when(scriptMapper.selectById(1L)).thenReturn(testScript);
            ArgumentCaptor<UserBrowseHistory> historyCaptor = ArgumentCaptor.forClass(UserBrowseHistory.class);

            recommendationService.recordBrowseHistory(1L, 1, 1L, 120);

            verify(browseHistoryMapper, times(1)).insert(historyCaptor.capture());
            verify(browseHistoryMapper, never()).updateById(any(UserBrowseHistory.class));
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "category_1", "1", 1.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "type_1", "1", 1.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "difficulty_2", "2", 1.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "tag_烧脑", "烧脑", 1.0);

            UserBrowseHistory history = historyCaptor.getValue();
            assertEquals(1L, history.getUserId());
            assertEquals(1, history.getTargetType());
            assertEquals(1L, history.getTargetId());
            assertEquals(120, history.getDuration());
            assertNotNull(history.getBrowseTime());
        }

        @Test
        @DisplayName("记录推荐点击")
        void testRecordRecommendationClick() {
            when(recommendationLogMapper.updateClickStatus(1L, 1L)).thenReturn(1);

            recommendationService.recordRecommendationClick(1L, 1L);

            verify(recommendationLogMapper, times(1)).updateClickStatus(1L, 1L);
        }

        @Test
        @DisplayName("记录推荐预约转化")
        void testRecordRecommendationReserve() {
            when(recommendationLogMapper.updateReserveStatus(1L, 1L)).thenReturn(1);

            recommendationService.recordRecommendationReserve(1L, 1L);

            verify(recommendationLogMapper, times(1)).updateReserveStatus(1L, 1L);
        }

        @Test
        @DisplayName("更新用户偏好 - 预约行为")
        void testUpdateUserPreference_Reservation() {
            doNothing().when(userPreferenceMapper).incrementPreference(anyLong(), anyString(), anyString(), anyDouble());
            when(scriptMapper.selectById(1L)).thenReturn(testScript);

            recommendationService.updateUserPreference(1L, 1L, 3);

            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "category_1", "1", 5.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "type_1", "1", 5.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "difficulty_2", "2", 5.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "tag_烧脑", "烧脑", 5.0);
        }

        @Test
        @DisplayName("更新用户偏好 - 新记录")
        void testUpdateUserPreference_NewRecord() {
            doNothing().when(userPreferenceMapper).incrementPreference(anyLong(), anyString(), anyString(), anyDouble());
            when(scriptMapper.selectById(1L)).thenReturn(testScript);

            recommendationService.updateUserPreference(1L, 1L, 2);

            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "category_1", "1", 3.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "type_1", "1", 3.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "difficulty_2", "2", 3.0);
            verify(userPreferenceMapper, times(1))
                    .incrementPreference(1L, "tag_烧脑", "烧脑", 3.0);
        }
    }

    @Nested
    @DisplayName("统计测试")
    class StatsTests {

        @Test
        @DisplayName("获取推荐效果统计")
        void testGetRecommendationStats() {
            when(recommendationLogMapper.getRecommendationStats(anyString())).thenReturn(Collections.emptyList());

            Map<String, Object> stats = recommendationService.getRecommendationStats(7);

            assertNotNull(stats);
        }
    }
}
