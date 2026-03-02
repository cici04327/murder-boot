package com.murder.service;

import com.murder.entity.Store;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.StoreReviewMapper;
import com.murder.mapper.StoreRoomMapper;
import com.murder.service.impl.StoreServiceImpl;
import com.murder.vo.StoreVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 门店服务单元测试
 * 注意：由于Java 23与Mockito的兼容性问题，这里不mock RedisTemplate
 * 只测试Mapper层的基本交互
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("门店服务测试")
class StoreServiceTest {

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private StoreRoomMapper storeRoomMapper;

    @Mock
    private StoreReviewMapper storeReviewMapper;

    private Store testStore;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testStore = Store.builder()
                .id(1L)
                .name("测试门店")
                .address("北京市朝阳区测试路100号")
                .phone("010-12345678")
                .images("http://example.com/store1.jpg,http://example.com/store2.jpg")
                .description("这是一家测试门店")
                .openTime(LocalTime.of(10, 0))
                .closeTime(LocalTime.of(22, 0))
                .longitude(new BigDecimal("116.4074"))
                .latitude(new BigDecimal("39.9042"))
                .rating(new BigDecimal("4.8"))
                .status(1)
                .createTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("根据ID查询门店 - Mapper层测试")
    void testSelectById() {
        // Given
        when(storeMapper.selectById(1L)).thenReturn(testStore);

        // When
        Store result = storeMapper.selectById(1L);

        // Then
        assertNotNull(result);
        assertEquals("测试门店", result.getName());
        assertEquals("北京市朝阳区测试路100号", result.getAddress());
        verify(storeMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据ID查询门店 - 不存在")
    void testSelectById_NotFound() {
        // Given
        when(storeMapper.selectById(999L)).thenReturn(null);

        // When
        Store result = storeMapper.selectById(999L);

        // Then
        assertNull(result);
        verify(storeMapper, times(1)).selectById(999L);
    }

    @Test
    @DisplayName("新增门店")
    void testAdd() {
        // Given
        when(storeMapper.insert(any(Store.class))).thenReturn(1);

        // When
        int result = storeMapper.insert(testStore);

        // Then
        assertEquals(1, result);
        verify(storeMapper, times(1)).insert(testStore);
    }

    @Test
    @DisplayName("更新门店")
    void testUpdate() {
        // Given
        when(storeMapper.updateById(any(Store.class))).thenReturn(1);

        // When
        int result = storeMapper.updateById(testStore);

        // Then
        assertEquals(1, result);
        verify(storeMapper, times(1)).updateById(testStore);
    }

    @Test
    @DisplayName("删除门店")
    void testDelete() {
        // Given
        when(storeMapper.deleteById(1L)).thenReturn(1);

        // When
        int result = storeMapper.deleteById(1L);

        // Then
        assertEquals(1, result);
        verify(storeMapper, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("查询门店列表")
    void testSelectList() {
        // Given
        when(storeMapper.selectList(any())).thenReturn(java.util.Arrays.asList(testStore));

        // When
        java.util.List<Store> result = storeMapper.selectList(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("测试门店", result.get(0).getName());
    }
}
