package com.murder.service;

import com.murder.common.context.BaseContext;
import com.murder.common.exception.BaseException;
import com.murder.dto.AiDmAssignDTO;
import com.murder.dto.AiDmConfirmDTO;
import com.murder.entity.AiScheduleTask;
import com.murder.entity.Dm;
import com.murder.entity.ScriptSchedule;
import com.murder.mapper.AiScheduleTaskMapper;
import com.murder.mapper.DmMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.ScriptScheduleMapper;
import com.murder.service.impl.AiScheduleServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AI排班服务测试")
class AiScheduleServiceTest {

    @Mock
    private ScriptScheduleMapper scriptScheduleMapper;
    @Mock
    private DmMapper dmMapper;
    @Mock
    private AiScheduleTaskMapper aiScheduleTaskMapper;
    @Mock
    private ScriptMapper scriptMapper;
    @Mock
    private RestTemplate restTemplate;

    private AiScheduleServiceImpl aiScheduleService;

    @BeforeEach
    void setUp() {
        aiScheduleService = new AiScheduleServiceImpl(
                scriptScheduleMapper,
                dmMapper,
                aiScheduleTaskMapper,
                scriptMapper,
                restTemplate
        );
        ReflectionTestUtils.setField(aiScheduleService, "aiApiKey", "");
        ReflectionTestUtils.setField(aiScheduleService, "aiModel", "deepseek-chat");
        ReflectionTestUtils.setField(aiScheduleService, "aiApiUrl", "https://example.com/mock");
    }

    @AfterEach
    void tearDown() {
        BaseContext.removeCurrentId();
    }

    @Test
    @DisplayName("门店管理员生成建议时强制使用上下文门店")
    void suggestDmAssignment_UsesStoreFromContext() {
        BaseContext.setRole("STORE_ADMIN");
        BaseContext.setStoreId(1L);

        AiDmAssignDTO dto = new AiDmAssignDTO();
        dto.setStoreId(999L);
        dto.setStartDate(LocalDate.of(2026, 4, 1));
        dto.setEndDate(LocalDate.of(2026, 4, 7));
        dto.setOnlyUnassigned(true);

        when(scriptScheduleMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(dmMapper.selectList(any())).thenReturn(Collections.emptyList());

        aiScheduleService.suggestDmAssignment(dto);

        ArgumentCaptor<AiScheduleTask> taskCaptor = ArgumentCaptor.forClass(AiScheduleTask.class);
        verify(aiScheduleTaskMapper).insert(taskCaptor.capture());
        assertEquals(1L, taskCaptor.getValue().getStoreId());
    }

    @Test
    @DisplayName("查询任务历史时门店管理员不能越权指定其他门店")
    void listTaskHistory_UsesStoreFromContext() {
        BaseContext.setRole("STORE_ADMIN");
        BaseContext.setStoreId(2L);
        when(aiScheduleTaskMapper.listByStore(2L, 10)).thenReturn(List.of());

        aiScheduleService.listTaskHistory(999L, 10);

        verify(aiScheduleTaskMapper).listByStore(2L, 10);
    }

    @Test
    @DisplayName("确认分配时禁止操作其他门店排期")
    void confirmDmAssignment_RejectsOtherStoreSchedule() {
        BaseContext.setRole("STORE_ADMIN");
        BaseContext.setStoreId(1L);

        AiDmConfirmDTO dto = new AiDmConfirmDTO();
        AiDmConfirmDTO.AssignItem item = new AiDmConfirmDTO.AssignItem();
        item.setScheduleId(10L);
        item.setDmId(20L);
        dto.setItems(List.of(item));

        ScriptSchedule schedule = new ScriptSchedule();
        schedule.setId(10L);
        schedule.setStoreId(2L);
        schedule.setIsDeleted(0);
        when(scriptScheduleMapper.selectById(10L)).thenReturn(schedule);

        assertThrows(BaseException.class, () -> aiScheduleService.confirmDmAssignment(dto));
        verify(dmMapper, never()).selectById(any());
        verify(scriptScheduleMapper, never()).updateById(any());
    }

    @Test
    @DisplayName("确认分配时禁止跨门店分配DM")
    void confirmDmAssignment_RejectsDmFromDifferentStore() {
        BaseContext.setRole("STORE_ADMIN");
        BaseContext.setStoreId(1L);

        AiDmConfirmDTO dto = new AiDmConfirmDTO();
        AiDmConfirmDTO.AssignItem item = new AiDmConfirmDTO.AssignItem();
        item.setScheduleId(10L);
        item.setDmId(20L);
        dto.setItems(List.of(item));

        ScriptSchedule schedule = new ScriptSchedule();
        schedule.setId(10L);
        schedule.setStoreId(1L);
        schedule.setIsDeleted(0);
        when(scriptScheduleMapper.selectById(10L)).thenReturn(schedule);

        Dm dm = new Dm();
        dm.setId(20L);
        dm.setStoreId(2L);
        dm.setStatus(1);
        dm.setIsDeleted(0);
        when(dmMapper.selectById(20L)).thenReturn(dm);

        assertThrows(BaseException.class, () -> aiScheduleService.confirmDmAssignment(dto));
        verify(scriptScheduleMapper, never()).updateById(any());
    }
}
