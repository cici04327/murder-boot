package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.Script;
import com.murder.mapper.ScriptMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 剧本模块集成测试
 */
@DisplayName("剧本模块集成测试")
class ScriptIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ScriptMapper scriptMapper;

    @Nested
    @DisplayName("剧本查询测试")
    class ScriptQueryTests {

        @Test
        @DisplayName("分页查询剧本列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(3))
                    .andExpect(jsonPath("$.data.records", hasSize(3)))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按分类查询剧本")
        void pageQuery_ByCategory() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("categoryId", "1"))  // 恐怖类
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].name").value("午夜惊魂"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按难度查询剧本")
        void pageQuery_ByDifficulty() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("difficulty", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].name").value("谜案追踪"));
        }

        @Test
        @DisplayName("按人数查询剧本")
        void pageQuery_ByPlayerCount() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("playerCount", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].name").value("午夜惊魂"));
        }

        @Test
        @DisplayName("根据ID查询剧本详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/script/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("午夜惊魂"))
                    .andExpect(jsonPath("$.data.categoryId").value(1))
                    .andExpect(jsonPath("$.data.price").value(198.00))
                    .andExpect(jsonPath("$.data.rating").value(4.5));
        }

        @Test
        @DisplayName("查询不存在的剧本")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/script/99999")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("获取热门剧本列表")
        void getHotScripts_Success() throws Exception {
            mockMvc.perform(get("/api/script/list/hot")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].name").value("谜案追踪"))
                    .andExpect(jsonPath("$.data[0].rating").value(4.8));
        }

        @Test
        @DisplayName("获取推荐剧本列表")
        void getRecommendedScripts_Success() throws Exception {
            mockMvc.perform(get("/api/script/list/recommended")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(3)))
                    .andExpect(jsonPath("$.data[0].name").value("谜案追踪"));
        }
    }

    @Nested
    @DisplayName("剧本分类测试")
    class CategoryTests {

        @Test
        @DisplayName("获取所有剧本分类")
        void getCategories_Success() throws Exception {
            mockMvc.perform(get("/api/script/category")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data", hasSize(4)))
                    .andExpect(jsonPath("$.data[0].name").value("恐怖"));
        }
    }

    @Nested
    @DisplayName("剧本管理测试")
    class ScriptManageTests {

        @Test
        @DisplayName("新增剧本")
        void addScript_Success() throws Exception {
            Script script = Script.builder()
                    .name("新测试剧本-集成")
                    .categoryId(2L)
                    .description("这是一个新的测试剧本")
                    .type(1)
                    .difficulty(2)
                    .playerCount(5)
                    .duration(new BigDecimal("3.5"))
                    .price(new BigDecimal("158.00"))
                    .status(1)
                    .build();

            mockMvc.perform(post("/api/script")
                            .header("token", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(script)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("新增成功"));

            LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Script::getName, "新测试剧本-集成");
            Script saved = scriptMapper.selectOne(wrapper);

            assertNotNull(saved);
            assertEquals(2L, saved.getCategoryId());
            assertEquals(5, saved.getPlayerCount());
            assertEquals(new BigDecimal("158.00"), saved.getPrice());
            assertEquals(1, saved.getStatus());
        }

        @Test
        @DisplayName("更新剧本信息")
        void updateScript_Success() throws Exception {
            Script script = Script.builder()
                    .id(1L)
                    .name("更新后的剧本名称")
                    .categoryId(1L)
                    .description("更新后的描述")
                    .type(1)
                    .difficulty(3)
                    .playerCount(6)
                    .duration(new BigDecimal("4.5"))
                    .price(new BigDecimal("218.00"))
                    .status(1)
                    .build();

            mockMvc.perform(put("/api/script")
                            .header("token", adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(script)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("更新成功"));

            Script updated = scriptMapper.selectById(1L);
            assertNotNull(updated);
            assertEquals("更新后的剧本名称", updated.getName());
            assertEquals("更新后的描述", updated.getDescription());
            assertEquals(3, updated.getDifficulty());
            assertEquals(new BigDecimal("218.00"), updated.getPrice());
        }

        @Test
        @DisplayName("删除剧本")
        void deleteScript_Success() throws Exception {
            // 删除ID为4的已下架剧本
            mockMvc.perform(delete("/api/script/4")
                            .header("token", adminToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("删除成功"));

            assertNull(scriptMapper.selectById(4L));
        }

        @Test
        @DisplayName("普通用户无权新增剧本")
        void addScript_RequiresSuperAdmin() throws Exception {
            Script script = Script.builder()
                    .name("普通用户尝试新增")
                    .categoryId(1L)
                    .description("无权限测试")
                    .type(1)
                    .difficulty(1)
                    .playerCount(6)
                    .duration(new BigDecimal("4.0"))
                    .price(new BigDecimal("100.00"))
                    .status(1)
                    .build();

            mockMvc.perform(post("/api/script")
                            .header("token", testUserToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(script)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value("无权限，仅超级管理员可执行此操作"));

            LambdaQueryWrapper<Script> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Script::getName, "普通用户尝试新增");
            assertEquals(0L, scriptMapper.selectCount(wrapper));
        }
    }

    @Nested
    @DisplayName("剧本搜索测试")
    class ScriptSearchTests {

        @Test
        @DisplayName("按关键字搜索剧本")
        void searchByKeyword_Success() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("keyword", "惊魂"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].name").value("午夜惊魂"));
        }

        @Test
        @DisplayName("组合条件搜索剧本")
        void searchByMultipleConditions_Success() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("categoryId", "1")
                            .param("difficulty", "3")
                            .param("playerCount", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].name").value("午夜惊魂"));
        }
    }
}
