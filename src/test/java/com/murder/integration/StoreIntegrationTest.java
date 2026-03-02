package com.murder.integration;

import com.murder.entity.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 门店模块集成测试
 */
@DisplayName("门店模块集成测试")
class StoreIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("门店查询测试")
    class StoreQueryTests {

        @Test
        @DisplayName("分页查询门店列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/store/page")
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @DisplayName("分页查询 - 带关键字搜索")
        void pageQuery_WithKeyword() throws Exception {
            mockMvc.perform(get("/api/store/page")
                            .param("page", "1")
                            .param("pageSize", "10")
                            .param("keyword", "测试门店A"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("根据ID查询门店详情")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/store/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("测试门店A"));
        }

        @Test
        @DisplayName("查询不存在的门店")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/store/99999"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test
        @DisplayName("获取热门门店列表")
        void getHotStores_Success() throws Exception {
            mockMvc.perform(get("/api/store/list/hot"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("获取附近门店")
        void getNearbyStores_Success() throws Exception {
            mockMvc.perform(get("/api/store/nearby")
                            .param("longitude", "116.4074")
                            .param("latitude", "39.9042")
                            .param("radius", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("门店管理测试")
    class StoreManageTests {

        @Test
        @DisplayName("新增门店")
        void addStore_Success() throws Exception {
            Store store = Store.builder()
                    .name("新测试门店")
                    .address("北京市东城区测试路400号")
                    .phone("010-99999999")
                    .description("这是新测试门店")
                    .longitude(new BigDecimal("116.4174"))
                    .latitude(new BigDecimal("39.9142"))
                    .status(1)
                    .build();

            mockMvc.perform(post("/api/store")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(store)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("更新门店信息")
        void updateStore_Success() throws Exception {
            Store store = Store.builder()
                    .id(1L)
                    .name("更新后的门店名称")
                    .address("北京市朝阳区更新路100号")
                    .phone("010-12345678")
                    .status(1)
                    .build();

            mockMvc.perform(put("/api/store")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(store)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("删除门店")
        void deleteStore_Success() throws Exception {
            // 删除ID为3的已关闭门店
            mockMvc.perform(delete("/api/store/3"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }
    }

    @Nested
    @DisplayName("门店详情查询测试")
    class StoreDetailTests {

        @Test
        @DisplayName("获取门店详细信息")
        void getStoreDetail_Success() throws Exception {
            mockMvc.perform(get("/api/store/detail/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.name").value("测试门店A"));
        }
    }
}
