package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.entity.Script;
import com.murder.entity.ScriptCategory;
import com.murder.entity.ScriptRole;
import com.murder.mapper.ScriptCategoryMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.ScriptRoleMapper;
import com.murder.service.impl.ScriptServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 剧本服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("剧本服务测试")
class ScriptServiceTest {

    @Mock
    private ScriptMapper scriptMapper;

    @Mock
    private ScriptCategoryMapper scriptCategoryMapper;

    @Mock
    private ScriptRoleMapper scriptRoleMapper;

    @InjectMocks
    private ScriptServiceImpl scriptService;

    private Script testScript;
    private ScriptCategory testCategory;
    private ScriptRole testRole;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
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
                .createTime(LocalDateTime.now())
                .build();

        testCategory = new ScriptCategory();
        testCategory.setId(1L);
        testCategory.setName("恐怖");
        testCategory.setStatus(1);

        testRole = new ScriptRole();
        testRole.setId(1L);
        testRole.setScriptId(1L);
        testRole.setRoleName("侦探");
        testRole.setGender(1);
    }

    @Test
    @DisplayName("根据ID查询剧本详情 - 成功")
    void testGetById_Success() {
        // Given
        when(scriptMapper.selectById(1L)).thenReturn(testScript);
        when(scriptCategoryMapper.selectById(1L)).thenReturn(testCategory);

        // When
        Script result = scriptService.getById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试剧本", result.getName());
        verify(scriptMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据ID查询剧本详情 - 不存在")
    void testGetById_NotFound() {
        // Given
        when(scriptMapper.selectById(999L)).thenReturn(null);

        // When
        Script result = scriptService.getById(999L);

        // Then
        assertNull(result);
        verify(scriptMapper, times(1)).selectById(999L);
    }

    @Test
    @DisplayName("获取热门剧本列表")
    void testGetHotScripts() {
        // Given
        List<Script> hotScripts = Arrays.asList(testScript);
        when(scriptMapper.selectList(any())).thenReturn(hotScripts);

        // When
        List<Script> result = scriptService.getHotScripts();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        verify(scriptMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("获取推荐剧本列表")
    void testGetRecommendedScripts() {
        // Given
        List<Script> recommendedScripts = Arrays.asList(testScript);
        when(scriptMapper.selectList(any())).thenReturn(recommendedScripts);

        // When
        List<Script> result = scriptService.getRecommendedScripts();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(scriptMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("新增剧本")
    void testAdd() {
        // Given
        when(scriptMapper.insert(any(Script.class))).thenReturn(1);

        // When
        scriptService.add(testScript);

        // Then
        verify(scriptMapper, times(1)).insert(testScript);
    }

    @Test
    @DisplayName("更新剧本")
    void testUpdate() {
        // Given
        when(scriptMapper.updateById(any(Script.class))).thenReturn(1);

        // When
        scriptService.update(testScript);

        // Then
        verify(scriptMapper, times(1)).updateById(testScript);
    }

    @Test
    @DisplayName("删除剧本")
    void testDelete() {
        // Given
        when(scriptMapper.deleteById(1L)).thenReturn(1);

        // When
        scriptService.delete(1L);

        // Then
        verify(scriptMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("获取剧本分类列表")
    void testGetCategories() {
        // Given
        List<ScriptCategory> categories = Arrays.asList(testCategory);
        when(scriptCategoryMapper.selectList(any())).thenReturn(categories);

        // When
        List<ScriptCategory> result = scriptService.getCategories();

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("恐怖", result.get(0).getName());
        verify(scriptCategoryMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("获取剧本角色列表")
    void testGetRolesByScriptId() {
        // Given
        List<ScriptRole> roles = Arrays.asList(testRole);
        when(scriptRoleMapper.selectList(any())).thenReturn(roles);

        // When
        List<ScriptRole> result = scriptService.getRolesByScriptId(1L);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("侦探", result.get(0).getRoleName());
        verify(scriptRoleMapper, times(1)).selectList(any());
    }
}
