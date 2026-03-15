package com.murder.service;

import com.murder.entity.ScriptRole;
import com.murder.mapper.ScriptRoleMapper;
import com.murder.service.impl.ScriptRoleServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 剧本角色服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("剧本角色服务测试")
class ScriptRoleServiceTest {

    @Mock
    private ScriptRoleMapper scriptRoleMapper;

    @InjectMocks
    private ScriptRoleServiceImpl scriptRoleService;

    private ScriptRole testRole;

    @BeforeEach
    void setUp() {
        testRole = new ScriptRole();
        testRole.setId(1L);
        testRole.setScriptId(10L);
        testRole.setRoleName("侦探A");
        testRole.setGender(1);
        testRole.setDescription("负责案件调查");
        testRole.setSort(1);
    }

    @Nested
    @DisplayName("查询测试")
    class QueryTests {

        @Test
        @DisplayName("按剧本ID查询角色列表 - 有数据")
        void testListByScriptId_WithData() {
            ScriptRole role2 = new ScriptRole();
            role2.setId(2L);
            role2.setScriptId(10L);
            role2.setRoleName("医生B");
            role2.setSort(2);

            when(scriptRoleMapper.selectList(any())).thenReturn(Arrays.asList(testRole, role2));

            List<ScriptRole> result = scriptRoleService.listByScriptId(10L);

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(scriptRoleMapper, times(1)).selectList(any());
        }

        @Test
        @DisplayName("按剧本ID查询角色列表 - 无数据")
        void testListByScriptId_Empty() {
            when(scriptRoleMapper.selectList(any())).thenReturn(Collections.emptyList());

            List<ScriptRole> result = scriptRoleService.listByScriptId(999L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("按ID查询角色详情 - 存在")
        void testGetById_Found() {
            when(scriptRoleMapper.selectById(1L)).thenReturn(testRole);

            ScriptRole result = scriptRoleService.getById(1L);

            assertNotNull(result);
            assertEquals("侦探A", result.getRoleName());
            verify(scriptRoleMapper, times(1)).selectById(1L);
        }

        @Test
        @DisplayName("按ID查询角色详情 - 不存在")
        void testGetById_NotFound() {
            when(scriptRoleMapper.selectById(999L)).thenReturn(null);

            ScriptRole result = scriptRoleService.getById(999L);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("新增测试")
    class AddTests {

        @Test
        @DisplayName("新增角色 - 成功")
        void testAdd_Success() {
            when(scriptRoleMapper.insert(any(ScriptRole.class))).thenReturn(1);
            ArgumentCaptor<ScriptRole> roleCaptor = ArgumentCaptor.forClass(ScriptRole.class);

            ScriptRole newRole = new ScriptRole();
            newRole.setScriptId(10L);
            newRole.setRoleName("新角色");

            scriptRoleService.add(newRole);
            verify(scriptRoleMapper, times(1)).insert(roleCaptor.capture());

            ScriptRole saved = roleCaptor.getValue();
            assertEquals(10L, saved.getScriptId());
            assertEquals("新角色", saved.getRoleName());
        }

        @Test
        @DisplayName("批量新增角色 - 成功")
        void testAddBatch_Success() {
            when(scriptRoleMapper.insert(any(ScriptRole.class))).thenReturn(1);
            ArgumentCaptor<ScriptRole> roleCaptor = ArgumentCaptor.forClass(ScriptRole.class);

            ScriptRole role1 = new ScriptRole();
            role1.setScriptId(10L);
            role1.setRoleName("角色1");

            ScriptRole role2 = new ScriptRole();
            role2.setScriptId(10L);
            role2.setRoleName("角色2");

            List<ScriptRole> roles = Arrays.asList(role1, role2);
            scriptRoleService.addBatch(roles);
            verify(scriptRoleMapper, times(2)).insert(roleCaptor.capture());

            List<ScriptRole> inserted = roleCaptor.getAllValues();
            assertEquals(Arrays.asList("角色1", "角色2"),
                    inserted.stream().map(ScriptRole::getRoleName).toList());
        }

        @Test
        @DisplayName("批量新增角色 - 空列表")
        void testAddBatch_EmptyList() {
            scriptRoleService.addBatch(Collections.emptyList());
            verify(scriptRoleMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("更新测试")
    class UpdateTests {

        @Test
        @DisplayName("更新角色 - 成功")
        void testUpdate_Success() {
            when(scriptRoleMapper.updateById(any(ScriptRole.class))).thenReturn(1);
            ArgumentCaptor<ScriptRole> roleCaptor = ArgumentCaptor.forClass(ScriptRole.class);

            testRole.setRoleName("更新后的角色名");
            scriptRoleService.update(testRole);
            verify(scriptRoleMapper, times(1)).updateById(roleCaptor.capture());

            ScriptRole updated = roleCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals("更新后的角色名", updated.getRoleName());
        }
    }

    @Nested
    @DisplayName("删除测试")
    class DeleteTests {

        @Test
        @DisplayName("删除角色 - 成功")
        void testDelete_Success() {
            when(scriptRoleMapper.deleteById(1L)).thenReturn(1);

            scriptRoleService.delete(1L);
            verify(scriptRoleMapper, times(1)).deleteById(1L);
        }

        @Test
        @DisplayName("删除不存在的角色 - 不抛异常")
        void testDelete_NotFound() {
            when(scriptRoleMapper.deleteById(999L)).thenReturn(0);

            scriptRoleService.delete(999L);
            verify(scriptRoleMapper, times(1)).deleteById(999L);
        }
    }
}
