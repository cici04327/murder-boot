package com.murder.service;

import com.murder.entity.StoreRoom;

import java.util.List;

/**
 * 门店房间服务接口
 */
public interface StoreRoomService {

    /**
     * 根据门店ID查询房间列表
     */
    List<StoreRoom> listByStoreId(Long storeId);

    /**
     * 根据ID查询房间详情
     */
    StoreRoom getById(Long id);

    /**
     * 新增房间
     */
    void add(StoreRoom room);

    /**
     * 更新房间
     */
    void update(StoreRoom room);

    /**
     * 删除房间
     */
    void delete(Long id);

    /**
     * 查询可用房间
     */
    List<StoreRoom> getAvailableRooms(Long storeId);
}
