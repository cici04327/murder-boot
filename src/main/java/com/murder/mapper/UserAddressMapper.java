package com.murder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.murder.entity.UserAddress;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户地址Mapper
 */
@Mapper
public interface UserAddressMapper extends BaseMapper<UserAddress> {
}
