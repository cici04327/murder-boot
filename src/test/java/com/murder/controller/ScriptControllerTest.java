package com.murder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.common.result.PageResult;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.service.ScriptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 剧本控制器单元测试（纯 Mockito，不依赖 Spring 容器）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("剧本控制器测试")
class ScriptControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ScriptService scriptService;

    @InjectMocks
    private ScriptController scriptController;

    private Script testScript;
    private ScriptCategory testCategory;

    @BeforeEach
    void setUp() {
        // 初始化 MockMvc 和 ObjectMapper
        mockMvc = MockMvcBuilders.standaloneSetup(scriptController).build();
        objectMapper = new ObjectMapper();
        testScript = Script.builder()
                .id(1L)
                .name("测试剧本")
                .categoryId(1L)
                .cover("http://example.com/cover.jpg")
                .description("这是一个测试剧本")
                .type(1)
                .difficulty(2)
                .playerCount(6)
                .duration(new BigDecimal("4.0"))
                .price(new BigDecimal("198.00"))
                .rating(new BigDecimal("4.5"))
                .status(1)
                .build();

        testCategory = new ScriptCategory();
        testCategory.setId(1L);
        testCategory.setName("恐怖");
        testCategory.setStatus(1);
    }

    @Test
    @DisplayName("分页查询剧本列表")
    void testPageQuery() throws Exception {
        // Given
        List<Script> scripts = Arrays.asList(testScript);
        PageResult<Script> pageResult = new PageResult<>(1L, scripts);
        when(scriptService.pageQuery(anyInt(), anyInt(), any(), any(), any(), any(), any(), any()))
                .thenReturn(pageResult);

        // When & Then
        mockMvc.perform(get("/api/script/page")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("测试剧本"));
    }

    @Test
    @DisplayName("查询剧本详情")
    void testGetById() throws Exception {
        // Given
        when(scriptService.getById(1L)).thenReturn(testScript);

        // When & Then
        mockMvc.perform(get("/api/script/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试剧本"));
    }

    @Test
    @DisplayName("获取热门剧本")
    void testGetHotScripts() throws Exception {
        // Given
        List<Script> hotScripts = Arrays.asList(testScript);
        when(scriptService.getHotScripts()).thenReturn(hotScripts);

        // When & Then
        mockMvc.perform(get("/api/script/list/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("测试剧本"));
    }

    @Test
    @DisplayName("获取推荐剧本")
    void testGetRecommendedScripts() throws Exception {
        // Given
        List<Script> recommendedScripts = Arrays.asList(testScript);
        when(scriptService.getRecommendedScripts()).thenReturn(recommendedScripts);

        // When & Then
        mockMvc.perform(get("/api/script/list/recommended"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取剧本分类列表")
    void testGetCategories() throws Exception {
        // Given
        List<ScriptCategory> categories = Arrays.asList(testCategory);
        when(scriptService.getCategories()).thenReturn(categories);

        // When & Then
        mockMvc.perform(get("/api/script/category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").value("恐怖"));
    }

    @Test
    @DisplayName("新增剧本")
    void testAdd() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/script")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testScript)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("新增成功"));
    }

    @Test
    @DisplayName("更新剧本")
    void testUpdate() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/script")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testScript)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("更新成功"));
    }

    @Test
    @DisplayName("删除剧本")
    void testDelete() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/script/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("删除成功"));
    }
}
