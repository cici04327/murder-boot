package com.murder.integration;

import com.murder.entity.Script;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 剧本模块集成测试
 */
@DisplayName("剧本模块集成测试")
class ScriptIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("剧本查询测试")
    class ScriptQueryTests {

        @Test
        @DisplayName("分页查询剧本列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按分类查询剧本")
        void pageQuery_ByCategory() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("categoryId", "1"))  // 恐怖类
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("按难度查询剧本")
        void pageQuery_ByDifficulty() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("difficulty", "2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("按人数查询剧本")
        void pageQuery_ByPlayerCount() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("playerCount", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("根据ID查询剧本详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/script/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("午夜惊魂"));
        }

        @Test
        @DisplayName("查询不存在的剧本")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/script/99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("获取热门剧本列表")
        void getHotScripts_Success() throws Exception {
            mockMvc.perform(get("/api/script/list/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("获取推荐剧本列表")
        void getRecommendedScripts_Success() throws Exception {
            mockMvc.perform(get("/api/script/list/recommended"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("剧本分类测试")
    class CategoryTests {

        @Test
        @DisplayName("获取所有剧本分类")
        void getCategories_Success() throws Exception {
            mockMvc.perform(get("/api/script/category"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].name").exists());
        }
    }

    @Nested
    @DisplayName("剧本管理测试")
    class ScriptManageTests {

        @Test
        @DisplayName("新增剧本")
        void addScript_Success() throws Exception {
            Script script = Script.builder()
                    .name("新测试剧本")
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(script)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
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
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(script)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("删除剧本")
        void deleteScript_Success() throws Exception {
            // 删除ID为4的已下架剧本
            mockMvc.perform(delete("/api/script/4"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("剧本搜索测试")
    class ScriptSearchTests {

        @Test
        @DisplayName("按关键字搜索剧本")
        void searchByKeyword_Success() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("keyword", "惊魂"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("组合条件搜索剧本")
        void searchByMultipleConditions_Success() throws Exception {
            mockMvc.perform(get("/api/script/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("categoryId", "1")
                            .param("difficulty", "3")
                            .param("playerCount", "6"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }
}
