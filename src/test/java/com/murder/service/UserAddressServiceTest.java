package com.murder.service;

import com.murder.dto.UserAddressDTO;
import com.murder.entity.UserAddress;
import com.murder.mapper.UserAddressMapper;
import com.murder.service.impl.UserAddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户地址服务测试类
 */
@ExtendWith(MockitoExtension.class)
public class UserAddressServiceTest {

    @InjectMocks
    private UserAddressServiceImpl userAddressService;

    @Mock
    private UserAddressMapper userAddressMapper;

    private Long testUserId = 1L;
    private Long testAddressId = 100L;

    /**
     * 测试 listByUserId - 查询地址列表：正常情况
     */
    @Test
    public void testListByUserIdSuccess() {
        UserAddress address1 = UserAddress.builder()
                .id(100L)
                .userId(testUserId)
                .contactName("张三")
                .contactPhone("13800138000")
                .province("北京市")
                .city("北京市")
                .district("朝阳区")
                .detail("某街道1号")
                .isDefault(1)
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .build();

        UserAddress address2 = UserAddress.builder()
                .id(101L)
                .userId(testUserId)
                .contactName("李四")
                .contactPhone("13900139000")
                .province("上海市")
                .city("上海市")
                .district("浦东新区")
                .detail("某街道2号")
                .isDefault(0)
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .build();

        List<UserAddress> addressList = Arrays.asList(address1, address2);

        when(userAddressMapper.selectList(any())).thenReturn(addressList);

        List<UserAddress> result = userAddressService.listByUserId(testUserId);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(userAddressMapper, times(1)).selectList(any());
    }

    /**
     * 测试 listByUserId - 查询地址列表：空列表
     */
    @Test
    public void testListByUserIdEmpty() {
        when(userAddressMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<UserAddress> result = userAddressService.listByUserId(testUserId);

        assertNotNull(result);
        assertEquals(0, result.size());
        verify(userAddressMapper, times(1)).selectList(any());
    }

    /**
     * 测试 getById - 查询单个地址：正常情况
     */
    @Test
    public void testGetByIdSuccess() {
        UserAddress address = UserAddress.builder()
                .id(testAddressId)
                .userId(testUserId)
                .contactName("张三")
                .contactPhone("13800138000")
                .province("北京市")
                .city("北京市")
                .district("朝阳区")
                .detail("某街道1号")
                .isDefault(1)
                .isDeleted(0)
                .createTime(LocalDateTime.now())
                .build();

        when(userAddressMapper.selectById(testAddressId)).thenReturn(address);

        UserAddress result = userAddressService.getById(testAddressId);

        assertNotNull(result);
        assertEquals(testAddressId, result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("张三", result.getContactName());
        verify(userAddressMapper, times(1)).selectById(testAddressId);
    }

    /**
     * 测试 getById - 查询单个地址：不存在返回 null
     */
    @Test
    public void testGetByIdNotExists() {
        when(userAddressMapper.selectById(testAddressId)).thenReturn(null);

        UserAddress result = userAddressService.getById(testAddressId);

        assertNull(result);
        verify(userAddressMapper, times(1)).selectById(testAddressId);
    }

    /**
     * 测试 add - 新增地址：正常添加，非默认
     */
    @Test
    public void testAddSuccess() {
        UserAddressDTO addressDTO = new UserAddressDTO();
        addressDTO.setUserId(testUserId);
        addressDTO.setContactName("张三");
        addressDTO.setContactPhone("13800138000");
        addressDTO.setProvince("北京市");
        addressDTO.setCity("北京市");
        addressDTO.setDistrict("朝阳区");
        addressDTO.setDetail("某街道1号");
        addressDTO.setIsDefault(0);

        when(userAddressMapper.insert(any(UserAddress.class))).thenReturn(1);

        userAddressService.add(addressDTO);

        verify(userAddressMapper, times(1)).insert(any(UserAddress.class));
    }

    /**
     * 测试 getDefaultAddress - 获取默认地址
     */
    @Test
    public void testGetDefaultAddress() {
        UserAddress defaultAddress = UserAddress.builder()
                .id(testAddressId)
                .userId(testUserId)
                .contactName("张三")
                .contactPhone("13800138000")
                .province("北京市")
                .city("北京市")
                .district("朝阳区")
                .detail("某街道1号")
                .isDefault(1)
                .isDeleted(0)
                .build();

        when(userAddressMapper.selectOne(any())).thenReturn(defaultAddress);

        UserAddress result = userAddressService.getDefaultAddress(testUserId);

        assertNotNull(result);
        assertEquals(1, result.getIsDefault());
        assertEquals(testUserId, result.getUserId());
        verify(userAddressMapper, times(1)).selectOne(any());
    }

    /**
     * 测试 delete - 删除地址：正常删除
     */
    @Test
    public void testDeleteSuccess() {
        when(userAddressMapper.deleteById(testAddressId)).thenReturn(1);

        userAddressService.delete(testAddressId);

        verify(userAddressMapper, times(1)).deleteById(testAddressId);
    }
}
