package com.murder.service;

import com.murder.dto.UserAddressDTO;
import com.murder.entity.UserAddress;

import java.util.List;

/**
 * 用户地址服务接口
 */
public interface UserAddressService {

    /**
     * 根据用户ID查询地址列表
     */
    List<UserAddress> listByUserId(Long userId);

    /**
     * 根据ID查询地址详情
     */
    UserAddress getById(Long id);

    /**
     * 新增地址
     */
    void add(UserAddressDTO addressDTO);

    /**
     * 更新地址
     */
    void update(UserAddressDTO addressDTO);

    /**
     * 删除地址
     */
    void delete(Long id);

    /**
     * 设置默认地址
     */
    void setDefault(Long id, Long userId);

    /**
     * 获取默认地址
     */
    UserAddress getDefaultAddress(Long userId);
}
