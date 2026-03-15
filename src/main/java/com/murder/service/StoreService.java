package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.StoreQueryDTO;
import com.murder.entity.Store;
import com.murder.vo.StoreDetailVO;
import com.murder.vo.StoreStatisticsVO;
import com.murder.vo.StoreVO;
import com.murder.vo.StoreLoginVO;
import com.murder.dto.StoreLoginDTO;

import java.util.List;

/**
 * 门店服务接口
 */
public interface StoreService {

    /**
     * 分页查询门店列表
     */
    PageResult<StoreVO> pageQuery(Integer page, Integer pageSize, String name);

    /**
     * 多条件分页查询门店列�?
     */
    PageResult<StoreVO> pageQueryAdvanced(StoreQueryDTO queryDTO);

    /**
     * 根据ID查询门店详情
     */
    StoreVO getById(Long id);

    /**
     * 根据ID查询门店详细信息（包含房间、员工、评价统计）
     */
    StoreDetailVO getDetailById(Long id);

    /**
     * 新增门店
     */
    void add(Store store);

    /**
     * 更新门店
     */
    void update(Store store);

    /**
     * 删除门店
     */
    void delete(Long id);

    /**
     * 批量删除门店
     */
    void batchDelete(List<Long> ids);

    /**
     * 更新门店状�?
     */
    void updateStatus(Long id, Integer status);

    /**
     * 批量更新门店状�?
     */
    void batchUpdateStatus(List<Long> ids, Integer status);

    /**
     * 获取门店统计信息（storeId为null时统计全部，否则统计指定门店）
     */
    StoreStatisticsVO getStatistics(Long storeId);

    /**
     * 获取所有门店列表（不分页）
     */
    List<StoreVO> listAll();
    
    /**
     * 获取热门门店
     */
    List<StoreVO> getHotStores();
    
    /**
     * 获取推荐门店
     */
    List<StoreVO> getRecommendedStores();
    
    /**
     * 获取附近门店（基于地理位置）
     */
    List<StoreVO> getNearbyStores(Double latitude, Double longitude, Integer limit);
    
    /**
     * 门店账号登录
     */
    StoreLoginVO storeLogin(StoreLoginDTO storeLoginDTO);
    
    /**
     * 更新门店登录密码
     */
    void updateLoginPassword(Long storeId, String oldPassword, String newPassword);
    
    /**
     * 更新门店账号信息（超级管理员使用）
     */
    void updateStoreAccount(Long storeId, String loginAccount, String loginPassword);
    
    /**
     * 重置门店密码为默认值
     */
    void resetStorePassword(Long storeId);
}
