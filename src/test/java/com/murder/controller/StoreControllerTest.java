package com.murder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.common.result.PageResult;
import com.murder.entity.Store;
import com.murder.service.StoreService;
import com.murder.vo.StoreDetailVO;
import com.murder.vo.StoreVO;
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
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 门店控制器单元测试（纯 Mockito，不依赖 Spring 容器）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("门店控制器测试")
class StoreControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private StoreController storeController;

    private StoreVO testStoreVO;
    private StoreDetailVO testStoreDetailVO;
    private Store testStore;

    @BeforeEach
    void setUp() {
        // 初始化 MockMvc 和 ObjectMapper
        mockMvc = MockMvcBuilders.standaloneSetup(storeController).build();
        objectMapper = new ObjectMapper();
        
        testStoreVO = new StoreVO();
        testStoreVO.setId(1L);
        testStoreVO.setName("测试门店");
        testStoreVO.setAddress("北京市朝阳区测试路100号");
        testStoreVO.setPhone("010-12345678");
        testStoreVO.setRating(new BigDecimal("4.8"));
        testStoreVO.setStatus(1);

        testStoreDetailVO = new StoreDetailVO();
        testStoreDetailVO.setId(1L);
        testStoreDetailVO.setName("测试门店");
        testStoreDetailVO.setAddress("北京市朝阳区测试路100号");
        testStoreDetailVO.setPhone("010-12345678");
        testStoreDetailVO.setDescription("这是一家测试门店");

        testStore = Store.builder()
                .id(1L)
                .name("测试门店")
                .address("北京市朝阳区测试路100号")
                .phone("010-12345678")
                .status(1)
                .build();
    }

    @Test
    @DisplayName("分页查询门店列表")
    void testPageQuery() throws Exception {
        // Given
        List<StoreVO> stores = Arrays.asList(testStoreVO);
        PageResult<StoreVO> pageResult = new PageResult<>(1L, stores);
        when(storeService.pageQuery(anyInt(), anyInt(), any())).thenReturn(pageResult);

        // When & Then
        mockMvc.perform(get("/api/store/page")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].name").value("测试门店"));
    }

    @Test
    @DisplayName("查询门店详情")
    void testGetById() throws Exception {
        // Given
        when(storeService.getById(1L)).thenReturn(testStoreVO);

        // When & Then
        mockMvc.perform(get("/api/store/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("测试门店"));
    }

    @Test
    @DisplayName("查询门店详情 - 增强版")
    void testGetDetailById() throws Exception {
        // Given
        when(storeService.getDetailById(1L)).thenReturn(testStoreDetailVO);

        // When & Then
        mockMvc.perform(get("/api/store/detail/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").value("测试门店"));
    }

    @Test
    @DisplayName("获取热门门店")
    void testGetHotStores() throws Exception {
        // Given
        List<StoreVO> hotStores = Arrays.asList(testStoreVO);
        when(storeService.getHotStores()).thenReturn(hotStores);

        // When & Then
        mockMvc.perform(get("/api/store/list/hot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("获取附近门店")
    void testGetNearbyStores() throws Exception {
        // Given
        List<StoreVO> nearbyStores = Arrays.asList(testStoreVO);
        when(storeService.getNearbyStores(any(), any(), any())).thenReturn(nearbyStores);

        // When & Then
        mockMvc.perform(get("/api/store/nearby")
                        .param("longitude", "116.4074")
                        .param("latitude", "39.9042")
                        .param("radius", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("新增门店")
    void testAdd() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testStore)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("更新门店")
    void testUpdate() throws Exception {
        // When & Then
        mockMvc.perform(put("/api/store")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testStore)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("删除门店")
    void testDelete() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/store/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
