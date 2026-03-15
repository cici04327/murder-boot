package com.murder.service;

import com.murder.entity.Dm;
import com.murder.entity.ScriptSchedule;
import com.murder.mapper.DmMapper;
import com.murder.mapper.ScriptScheduleMapper;
import com.murder.service.impl.ScriptScheduleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * 剧本排期服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScriptScheduleService 测试")
class ScriptScheduleServiceTest {

    @Mock
    private ScriptScheduleMapper scriptScheduleMapper;

    @Mock
    private DmMapper dmMapper;

    @InjectMocks
    private ScriptScheduleServiceImpl scriptScheduleService;

    private ScriptSchedule testSchedule;

    @BeforeEach
    void setUp() {
        testSchedule = ScriptSchedule.builder()
                .id(1L)
                .storeId(100L)
                .scriptId(10L)
                .roomId(1L)
                .scheduleDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .maxPlayers(8)
                .currentPlayers(0)
                .status(1)
                .dmId(null)
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("listByStoreAndDate - 按门店和日期查询排期：正常返回")
    void testListByStoreAndDateNormal() {
        // Arrange
        Long storeId = 100L;
        LocalDate scheduleDate = LocalDate.now().plusDays(1);
        List<ScriptSchedule> scheduleList = new ArrayList<>();
        scheduleList.add(testSchedule);
        
        when(scriptScheduleMapper.listByStoreAndDate(storeId, scheduleDate))
                .thenReturn(scheduleList);
        when(dmMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        // Act
        List<ScriptSchedule> result = scriptScheduleService.listByStoreAndDate(storeId, scheduleDate);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getScriptId());
        verify(scriptScheduleMapper, times(1)).listByStoreAndDate(storeId, scheduleDate);
    }

    @Test
    @DisplayName("listByStoreAndDate - 按门店和日期查询排期：返回空列表")
    void testListByStoreAndDateEmpty() {
        // Arrange
        Long storeId = 999L;
        LocalDate scheduleDate = LocalDate.now().plusDays(1);
        when(scriptScheduleMapper.listByStoreAndDate(storeId, scheduleDate))
                .thenReturn(Collections.emptyList());

        // Act
        List<ScriptSchedule> result = scriptScheduleService.listByStoreAndDate(storeId, scheduleDate);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(scriptScheduleMapper, times(1)).listByStoreAndDate(storeId, scheduleDate);
    }

    @Test
    @DisplayName("add - 新增排期：正常添加")
    void testAddNormal() {
        // Arrange
        ScriptSchedule newSchedule = ScriptSchedule.builder()
                .storeId(100L)
                .scriptId(10L)
                .roomId(1L)
                .scheduleDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .maxPlayers(8)
                .build();
        
        when(scriptScheduleMapper.selectCount(any())).thenReturn(0L);
        when(scriptScheduleMapper.insert(any(ScriptSchedule.class))).thenReturn(1);

        // Act
        scriptScheduleService.add(newSchedule);

        // Assert
        verify(scriptScheduleMapper, times(1)).selectCount(any());
        verify(scriptScheduleMapper, times(1)).insert(any(ScriptSchedule.class));
        assertEquals(0, newSchedule.getCurrentPlayers());
        assertEquals(1, newSchedule.getStatus());
        assertEquals(0, newSchedule.getIsDeleted());
    }

    @Test
    @DisplayName("add - 新增排期：时间冲突抛异常")
    void testAddTimeConflict() {
        // Arrange
        ScriptSchedule conflictSchedule = ScriptSchedule.builder()
                .storeId(100L)
                .scriptId(10L)
                .roomId(1L)
                .scheduleDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .maxPlayers(8)
                .build();
        
        // 模拟冲突检测返回存在冲突
        when(scriptScheduleMapper.selectCount(any())).thenReturn(1L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            scriptScheduleService.add(conflictSchedule);
        });
        assertTrue(exception.getMessage().contains("时段已有排期"));
        verify(scriptScheduleMapper, times(1)).selectCount(any());
        verify(scriptScheduleMapper, never()).insert(any(ScriptSchedule.class));
    }

    @Test
    @DisplayName("incrementCurrentPlayers - 增加预约人数：正常增加")
    void testIncrementCurrentPlayersNormal() {
        // Arrange
        Long scheduleId = 1L;
        int count = 3;
        ScriptSchedule schedule = ScriptSchedule.builder()
                .id(scheduleId)
                .currentPlayers(2)
                .maxPlayers(8)
                .status(1)
                .build();
        
        when(scriptScheduleMapper.selectById(scheduleId)).thenReturn(schedule);
        when(scriptScheduleMapper.updateById(any(ScriptSchedule.class))).thenReturn(1);

        // Act
        scriptScheduleService.incrementCurrentPlayers(scheduleId, count);

        // Assert
        verify(scriptScheduleMapper, times(1)).selectById(scheduleId);
        verify(scriptScheduleMapper, times(1)).updateById(any(ScriptSchedule.class));
    }

    @Test
    @DisplayName("incrementCurrentPlayers - 增加预约人数：满员自动关闭")
    void testIncrementCurrentPlayersFullAutoClose() {
        // Arrange
        Long scheduleId = 1L;
        int count = 6; // 加上已有的2人，共8人，达到上限
        ScriptSchedule schedule = ScriptSchedule.builder()
                .id(scheduleId)
                .currentPlayers(2)
                .maxPlayers(8)
                .status(1)
                .build();
        
        when(scriptScheduleMapper.selectById(scheduleId)).thenReturn(schedule);
        when(scriptScheduleMapper.updateById(any(ScriptSchedule.class))).thenReturn(1);

        // Act
        scriptScheduleService.incrementCurrentPlayers(scheduleId, count);

        // Assert
        verify(scriptScheduleMapper, times(1)).selectById(scheduleId);
        verify(scriptScheduleMapper, times(1)).updateById(argThat(arg -> {
            return arg.getId().equals(scheduleId) && arg.getCurrentPlayers() == 8;
        }));
    }

    @Test
    @DisplayName("decrementCurrentPlayers - 减少预约人数：正常减少")
    void testDecrementCurrentPlayersNormal() {
        // Arrange
        Long scheduleId = 1L;
        int count = 2;
        ScriptSchedule schedule = ScriptSchedule.builder()
                .id(scheduleId)
                .currentPlayers(5)
                .status(1)
                .build();
        
        when(scriptScheduleMapper.selectById(scheduleId)).thenReturn(schedule);
        when(scriptScheduleMapper.updateById(any(ScriptSchedule.class))).thenReturn(1);

        // Act
        scriptScheduleService.decrementCurrentPlayers(scheduleId, count);

        // Assert
        verify(scriptScheduleMapper, times(1)).selectById(scheduleId);
        verify(scriptScheduleMapper, times(1)).updateById(any(ScriptSchedule.class));
    }

    @Test
    @DisplayName("decrementCurrentPlayers - 减少预约人数：从已满状态恢复")
    void testDecrementCurrentPlayersRecoverFromFull() {
        // Arrange
        Long scheduleId = 1L;
        int count = 1;
        ScriptSchedule schedule = ScriptSchedule.builder()
                .id(scheduleId)
                .currentPlayers(8)
                .status(0) // 已满状态
                .build();
        
        when(scriptScheduleMapper.selectById(scheduleId)).thenReturn(schedule);
        when(scriptScheduleMapper.updateById(any(ScriptSchedule.class))).thenReturn(1);

        // Act
        scriptScheduleService.decrementCurrentPlayers(scheduleId, count);

        // Assert
        verify(scriptScheduleMapper, times(1)).selectById(scheduleId);
        verify(scriptScheduleMapper, times(1)).updateById(argThat(arg -> {
            return arg.getId().equals(scheduleId) && arg.getStatus() == 1;
        }));
    }

    @Test
    @DisplayName("getAvailableSchedules - 查询可用排期：正常返回")
    void testGetAvailableSchedulesNormal() {
        // Arrange
        Long scriptId = 10L;
        Long storeId = 100L;
        Integer days = 7;
        
        ScriptSchedule availableSchedule = ScriptSchedule.builder()
                .id(1L)
                .scriptId(scriptId)
                .storeId(storeId)
                .scheduleDate(LocalDate.now().plusDays(1))
                .startTime(LocalTime.of(14, 0))
                .maxPlayers(8)
                .currentPlayers(3)
                .status(1)
                .isDeleted(0)
                .build();
        
        List<ScriptSchedule> scheduleList = new ArrayList<>();
        scheduleList.add(availableSchedule);
        
        when(scriptScheduleMapper.selectList(any())).thenReturn(scheduleList);
        when(dmMapper.selectBatchIds(any())).thenReturn(Collections.emptyList());

        // Act
        List<ScriptSchedule> result = scriptScheduleService.getAvailableSchedules(scriptId, storeId, days);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getStatus());
        assertTrue(result.get(0).getCurrentPlayers() < result.get(0).getMaxPlayers());
        verify(scriptScheduleMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getAvailableSchedules - 查询可用排期：无可用场次")
    void testGetAvailableSchedulesEmpty() {
        // Arrange
        Long scriptId = 10L;
        Long storeId = 100L;
        Integer days = 7;
        
        when(scriptScheduleMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        List<ScriptSchedule> result = scriptScheduleService.getAvailableSchedules(scriptId, storeId, days);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(scriptScheduleMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getAvailableSchedules - 查询可用排期：使用默认days=7")
    void testGetAvailableSchedulesDefaultDays() {
        // Arrange
        when(scriptScheduleMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        List<ScriptSchedule> result = scriptScheduleService.getAvailableSchedules(null, null, null);

        // Assert
        assertNotNull(result);
        verify(scriptScheduleMapper, times(1)).selectList(any());
    }
}
