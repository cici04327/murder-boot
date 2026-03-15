package com.murder.service;

import com.murder.entity.StoreRoom;
import com.murder.mapper.StoreRoomMapper;
import com.murder.service.impl.StoreRoomServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 门店房间服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreRoomService 测试")
class StoreRoomServiceTest {

    @Mock
    private StoreRoomMapper storeRoomMapper;

    @InjectMocks
    private StoreRoomServiceImpl storeRoomService;

    private StoreRoom testRoom;

    @BeforeEach
    void setUp() {
        testRoom = StoreRoom.builder()
                .id(1L)
                .storeId(100L)
                .name("VIP房间")
                .type(1)
                .capacity(8)
                .description("豪华VIP房间")
                .status(1)
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("getByStoreId - 查询门店房间列表：正常返回")
    void testListByStoreIdNormal() {
        // Arrange
        Long storeId = 100L;
        List<StoreRoom> roomList = new ArrayList<>();
        roomList.add(testRoom);
        when(storeRoomMapper.selectList(any())).thenReturn(roomList);

        // Act
        List<StoreRoom> result = storeRoomService.listByStoreId(storeId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("VIP房间", result.get(0).getName());
        verify(storeRoomMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("listByStoreId - 查询门店房间列表：返回空列表")
    void testListByStoreIdEmpty() {
        // Arrange
        Long storeId = 999L;
        when(storeRoomMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        List<StoreRoom> result = storeRoomService.listByStoreId(storeId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(storeRoomMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getById - 查询单个房间：正常返回")
    void testGetByIdNormal() {
        // Arrange
        when(storeRoomMapper.selectById(1L)).thenReturn(testRoom);

        // Act
        StoreRoom result = storeRoomService.getById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("VIP房间", result.getName());
        verify(storeRoomMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("getById - 查询单个房间：房间不存在")
    void testGetByIdNotFound() {
        // Arrange
        when(storeRoomMapper.selectById(999L)).thenReturn(null);

        // Act
        StoreRoom result = storeRoomService.getById(999L);

        // Assert
        assertNull(result);
        verify(storeRoomMapper, times(1)).selectById(999L);
    }

    @Test
    @DisplayName("add - 新增房间：正常添加")
    void testAddNormal() {
        // Arrange
        StoreRoom newRoom = StoreRoom.builder()
                .storeId(100L)
                .name("新房间")
                .type(2)
                .capacity(10)
                .description("新增房间")
                .status(1)
                .isDeleted(0)
                .build();
        when(storeRoomMapper.insert(any(StoreRoom.class))).thenReturn(1);

        // Act
        storeRoomService.add(newRoom);

        // Assert
        verify(storeRoomMapper, times(1)).insert(any(StoreRoom.class));
    }

    @Test
    @DisplayName("update - 更新房间：正常更新")
    void testUpdateNormal() {
        // Arrange
        StoreRoom updateRoom = StoreRoom.builder()
                .id(1L)
                .storeId(100L)
                .name("更新后的房间名")
                .type(1)
                .capacity(8)
                .status(1)
                .build();
        when(storeRoomMapper.updateById(any(StoreRoom.class))).thenReturn(1);

        // Act
        storeRoomService.update(updateRoom);

        // Assert
        verify(storeRoomMapper, times(1)).updateById(any(StoreRoom.class));
    }

    @Test
    @DisplayName("update - 更新房间：房间不存在抛异常")
    void testUpdateNotFound() {
        // Arrange
        StoreRoom updateRoom = StoreRoom.builder()
                .id(999L)
                .name("不存在的房间")
                .build();
        when(storeRoomMapper.updateById(any(StoreRoom.class))).thenReturn(0);

        // Act
        storeRoomService.update(updateRoom);

        // Assert
        verify(storeRoomMapper, times(1)).updateById(any(StoreRoom.class));
    }

    @Test
    @DisplayName("delete - 删除房间：正常删除")
    void testDeleteNormal() {
        // Arrange
        Long roomId = 1L;
        when(storeRoomMapper.deleteById(roomId)).thenReturn(1);

        // Act
        storeRoomService.delete(roomId);

        // Assert
        verify(storeRoomMapper, times(1)).deleteById(roomId);
    }

    @Test
    @DisplayName("getAvailableRooms - 查询可用房间：正常返回")
    void testGetAvailableRoomsNormal() {
        // Arrange
        Long storeId = 100L;
        StoreRoom availableRoom1 = StoreRoom.builder()
                .id(1L)
                .storeId(storeId)
                .name("房间1")
                .status(1)
                .build();
        StoreRoom availableRoom2 = StoreRoom.builder()
                .id(2L)
                .storeId(storeId)
                .name("房间2")
                .status(1)
                .build();
        List<StoreRoom> availableList = new ArrayList<>();
        availableList.add(availableRoom1);
        availableList.add(availableRoom2);
        when(storeRoomMapper.selectList(any())).thenReturn(availableList);

        // Act
        List<StoreRoom> result = storeRoomService.getAvailableRooms(storeId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(r -> r.getStatus() == 1));
        verify(storeRoomMapper, times(1)).selectList(any());
    }

    @Test
    @DisplayName("getAvailableRooms - 查询可用房间：无可用房间")
    void testGetAvailableRoomsEmpty() {
        // Arrange
        Long storeId = 100L;
        when(storeRoomMapper.selectList(any())).thenReturn(Collections.emptyList());

        // Act
        List<StoreRoom> result = storeRoomService.getAvailableRooms(storeId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(storeRoomMapper, times(1)).selectList(any());
    }
}
