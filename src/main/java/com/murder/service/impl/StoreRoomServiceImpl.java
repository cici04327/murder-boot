package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.entity.StoreRoom;
import com.murder.mapper.StoreRoomMapper;
import com.murder.service.StoreRoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 门店房间服务实现�?
 */
@Service
public class StoreRoomServiceImpl implements StoreRoomService {

    @Autowired
    private StoreRoomMapper storeRoomMapper;

    /**
     * 根据门店ID查询房间列表
     */
    @Override
    public List<StoreRoom> listByStoreId(Long storeId) {
        LambdaQueryWrapper<StoreRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreRoom::getStoreId, storeId);
        wrapper.orderByAsc(StoreRoom::getType);
        return storeRoomMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询房间详情
     */
    @Override
    public StoreRoom getById(Long id) {
        return storeRoomMapper.selectById(id);
    }

    /**
     * 新增房间
     */
    @Override
    public void add(StoreRoom room) {
        storeRoomMapper.insert(room);
    }

    /**
     * 更新房间
     */
    @Override
    public void update(StoreRoom room) {
        storeRoomMapper.updateById(room);
    }

    /**
     * 删除房间
     */
    @Override
    public void delete(Long id) {
        storeRoomMapper.deleteById(id);
    }

    /**
     * 查询可用房间
     */
    @Override
    public List<StoreRoom> getAvailableRooms(Long storeId) {
        LambdaQueryWrapper<StoreRoom> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(StoreRoom::getStoreId, storeId);
        wrapper.eq(StoreRoom::getStatus, 1); // 只查询可用房�?
        wrapper.orderByAsc(StoreRoom::getType);
        return storeRoomMapper.selectList(wrapper);
    }
}
