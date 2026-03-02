package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.murder.dto.UserAddressDTO;
import com.murder.entity.UserAddress;
import com.murder.mapper.UserAddressMapper;
import com.murder.service.UserAddressService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户地址服务实现�?
 */
@Service
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    /**
     * 根据用户ID查询地址列表
     */
    @Override
    public List<UserAddress> listByUserId(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId);
        wrapper.orderByDesc(UserAddress::getIsDefault);
        wrapper.orderByDesc(UserAddress::getCreateTime);
        return userAddressMapper.selectList(wrapper);
    }

    /**
     * 根据ID查询地址详情
     */
    @Override
    public UserAddress getById(Long id) {
        return userAddressMapper.selectById(id);
    }

    /**
     * 新增地址
     */
    @Override
    @Transactional
    public void add(UserAddressDTO addressDTO) {
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(addressDTO, address);
        
        // 如果设置为默认地址，先将该用户的其他地址设为非默�?
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(address.getUserId());
        }
        
        userAddressMapper.insert(address);
    }

    /**
     * 更新地址
     */
    @Override
    @Transactional
    public void update(UserAddressDTO addressDTO) {
        UserAddress address = new UserAddress();
        BeanUtils.copyProperties(addressDTO, address);
        
        // 如果设置为默认地址，先将该用户的其他地址设为非默�?
        if (address.getIsDefault() != null && address.getIsDefault() == 1) {
            clearDefaultAddress(address.getUserId());
        }
        
        userAddressMapper.updateById(address);
    }

    /**
     * 删除地址
     */
    @Override
    public void delete(Long id) {
        userAddressMapper.deleteById(id);
    }

    /**
     * 设置默认地址
     */
    @Override
    @Transactional
    public void setDefault(Long id, Long userId) {
        // 先将该用户的所有地址设为非默�?
        clearDefaultAddress(userId);
        
        // 设置指定地址为默�?
        UserAddress address = new UserAddress();
        address.setId(id);
        address.setIsDefault(1);
        userAddressMapper.updateById(address);
    }

    /**
     * 获取默认地址
     */
    @Override
    public UserAddress getDefaultAddress(Long userId) {
        LambdaQueryWrapper<UserAddress> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId);
        wrapper.eq(UserAddress::getIsDefault, 1);
        return userAddressMapper.selectOne(wrapper);
    }

    /**
     * 清除用户的默认地址
     */
    private void clearDefaultAddress(Long userId) {
        LambdaUpdateWrapper<UserAddress> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserAddress::getUserId, userId);
        wrapper.set(UserAddress::getIsDefault, 0);
        userAddressMapper.update(null, wrapper);
    }
}
