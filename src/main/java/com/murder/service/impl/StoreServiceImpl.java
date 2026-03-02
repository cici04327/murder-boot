package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.constant.JwtClaimsConstant;
import com.murder.common.constant.RedisConstant;
import com.murder.common.exception.PasswordErrorException;
import com.murder.common.properties.JwtProperties;
import com.murder.common.result.PageResult;
import com.murder.common.utils.JwtUtil;
import com.murder.dto.StoreLoginDTO;
import com.murder.dto.StoreQueryDTO;
import com.murder.entity.Store;
import com.murder.vo.StoreLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import com.murder.entity.StoreRoom;
import com.murder.vo.StoreDetailVO;
import com.murder.vo.StoreStatisticsVO;
import com.murder.vo.StoreVO;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.StoreReviewMapper;
import com.murder.mapper.StoreRoomMapper;
import com.murder.service.StoreService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 门店服务实现类
 */
@Slf4j
@Service
public class StoreServiceImpl implements StoreService {
    
    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private StoreMapper storeMapper;
    
    @Autowired
    private StoreRoomMapper storeRoomMapper;
    
    @Autowired
    private StoreReviewMapper storeReviewMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 分页查询门店列表
     */
    @Override
    public PageResult<StoreVO> pageQuery(Integer page, Integer pageSize, String name) {
        Page<Store> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(name)) {
            wrapper.like(Store::getName, name);
        }
        wrapper.orderByDesc(Store::getCreateTime);
        
        // 手动查询总数
        Long total = storeMapper.selectCount(wrapper);
        
        storeMapper.selectPage(pageInfo, wrapper);
        
        List<StoreVO> storeVOList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 使用手动查询�?total �?
        return new PageResult<>(total, storeVOList);
    }

    /**
     * 根据ID查询门店详情
     */
    @Override
    @Cacheable(value = "store", key = "#id", unless = "#result == null")
    public StoreVO getById(Long id) {
        // 先从Redis缓存中获�?
        String cacheKey = RedisConstant.STORE_CACHE_PREFIX + id;
        StoreVO cachedStore = (StoreVO) redisTemplate.opsForValue().get(cacheKey);
        
        if (cachedStore != null) {
            return cachedStore;
        }
        
        // 缓存未命中，从数据库查询
        Store store = storeMapper.selectById(id);
        StoreVO storeVO = convertToVO(store);
        
        // 存入缓存
        if (storeVO != null) {
            redisTemplate.opsForValue().set(cacheKey, storeVO, RedisConstant.CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        }
        
        return storeVO;
    }

    /**
     * 新增门店
     */
    @Override
    public void add(Store store) {
        storeMapper.insert(store);
    }

    /**
     * 更新门店
     */
    @Override
    @CacheEvict(value = {"store", "store:detail", "hot:stores"}, allEntries = true)
    public void update(Store store) {
        storeMapper.updateById(store);
        // 删除缓存
        String cacheKey = RedisConstant.STORE_CACHE_PREFIX + store.getId();
        redisTemplate.delete(cacheKey);
    }

    /**
     * 删除门店
     */
    @Override
    @CacheEvict(value = "store", key = "#id")
    public void delete(Long id) {
        storeMapper.deleteById(id);
        // 删除缓存
        String cacheKey = RedisConstant.STORE_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
    }

    @Override
    public PageResult<StoreVO> pageQueryAdvanced(StoreQueryDTO queryDTO) {
        Page<Store> pageInfo = new Page<>(queryDTO.getPage(), queryDTO.getPageSize());
        
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        
        // 门店名称模糊查询
        if (StringUtils.hasText(queryDTO.getName())) {
            wrapper.like(Store::getName, queryDTO.getName());
        }
        
        // 地址模糊查询
        if (StringUtils.hasText(queryDTO.getAddress())) {
            wrapper.like(Store::getAddress, queryDTO.getAddress());
        }
        
        // 状态查�?- 默认只查询营业中的门�?
        if (queryDTO.getStatus() != null) {
            wrapper.eq(Store::getStatus, queryDTO.getStatus());
        } else {
            wrapper.eq(Store::getStatus, 1);
        }
        
        // 评分范围查询
        if (queryDTO.getMinRating() != null) {
            wrapper.ge(Store::getRating, queryDTO.getMinRating());
        }
        if (queryDTO.getMaxRating() != null) {
            wrapper.le(Store::getRating, queryDTO.getMaxRating());
        }
        
        // 手动查询总数
        Long total = storeMapper.selectCount(wrapper);
        
        // 根据排序方式排序
        String sortBy = queryDTO.getSortBy();
        if ("rating".equals(sortBy)) {
            // 评分最高：按评分降序，评分相同按创建时间降�?
            wrapper.orderByDesc(Store::getRating)
                   .orderByDesc(Store::getCreateTime);
        } else if ("hot".equals(sortBy)) {
            // 最热门：按创建时间降序（可以后续改为按预约数量等）
            wrapper.orderByDesc(Store::getCreateTime)
                   .orderByDesc(Store::getRating);
        } else if ("distance".equals(sortBy)) {
            // 距离最近：如果提供了经纬度，按距离排序；否则按默认排序
            if (queryDTO.getLongitude() != null && queryDTO.getLatitude() != null) {
                // 先查询所有数�?
                wrapper.orderByDesc(Store::getRating);
            } else {
                // 没有经纬度信息，按评分排�?
                wrapper.orderByDesc(Store::getRating)
                       .orderByDesc(Store::getCreateTime);
            }
        } else {
            // 默认排序：按评分降序
            wrapper.orderByDesc(Store::getRating)
                   .orderByDesc(Store::getCreateTime);
        }
        
        storeMapper.selectPage(pageInfo, wrapper);
        
        List<StoreVO> storeVOList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 如果是距离排序且提供了经纬度，需要计算距离并重新排序
        if ("distance".equals(sortBy) && queryDTO.getLongitude() != null && queryDTO.getLatitude() != null) {
            storeVOList = storeVOList.stream()
                    .peek(storeVO -> {
                        // 计算距离并存储在VO�?
                        if (storeVO.getLongitude() != null && storeVO.getLatitude() != null) {
                            double distance = calculateDistance(
                                    queryDTO.getLatitude().doubleValue(),
                                    queryDTO.getLongitude().doubleValue(),
                                    storeVO.getLatitude().doubleValue(),
                                    storeVO.getLongitude().doubleValue()
                            );
                            storeVO.setDistance(distance);
                        }
                    })
                    .sorted((s1, s2) -> {
                        // 按距离排�?
                        if (s1.getDistance() == null) return 1;
                        if (s2.getDistance() == null) return -1;
                        return Double.compare(s1.getDistance(), s2.getDistance());
                    })
                    .collect(Collectors.toList());
        }
        
        // 使用手动查询�?total �?
        return new PageResult<>(total, storeVOList);
    }
    
    /**
     * 计算两点之间的距离（单位：公里）
     * 使用Haversine公式计算地球表面两点间的距离
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }

    @Override
    @Cacheable(value = "store:detail", key = "#id", unless = "#result == null")
    public StoreDetailVO getDetailById(Long id) {
        Store store = storeMapper.selectById(id);
        if (store == null) {
            return null;
        }
        
        // 查询房间列表
        LambdaQueryWrapper<StoreRoom> roomWrapper = new LambdaQueryWrapper<>();
        roomWrapper.eq(StoreRoom::getStoreId, id);
        List<StoreRoom> rooms = storeRoomMapper.selectList(roomWrapper);
        
        // 转换房间信息
        List<StoreDetailVO.RoomInfo> roomInfos = rooms.stream()
                .map(room -> StoreDetailVO.RoomInfo.builder()
                        .id(room.getId())
                        .name(room.getName())
                        .type(room.getType())
                        .capacity(room.getCapacity())
                        .description(room.getDescription())
                        .status(room.getStatus())
                        .build())
                .collect(Collectors.toList());
        
        // 统计评价数量
        Long reviewCount = storeReviewMapper.countByStoreId(id);
        
        // 构建详情VO
        StoreDetailVO detailVO = StoreDetailVO.builder()
                .id(store.getId())
                .name(store.getName())
                .address(store.getAddress())
                .phone(store.getPhone())
                .images(store.getImages())
                .description(store.getDescription())
                .openTime(store.getOpenTime() != null ? store.getOpenTime().toString() : null)
                .closeTime(store.getCloseTime() != null ? store.getCloseTime().toString() : null)
                .longitude(store.getLongitude())
                .latitude(store.getLatitude())
                .rating(store.getRating())
                .status(store.getStatus())
                .rooms(roomInfos)
                .reviewCount(reviewCount.intValue())
                .build();
        
        return detailVO;
    }

    @Override
    public void batchDelete(List<Long> ids) {
        storeMapper.deleteBatchIds(ids);
        // 批量删除缓存
        ids.forEach(id -> {
            String cacheKey = RedisConstant.STORE_CACHE_PREFIX + id;
            redisTemplate.delete(cacheKey);
        });
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Store store = new Store();
        store.setId(id);
        store.setStatus(status);
        storeMapper.updateById(store);
        
        // 删除缓存
        String cacheKey = RedisConstant.STORE_CACHE_PREFIX + id;
        redisTemplate.delete(cacheKey);
    }

    @Override
    public void batchUpdateStatus(List<Long> ids, Integer status) {
        ids.forEach(id -> updateStatus(id, status));
    }

    @Override
    public StoreStatisticsVO getStatistics() {
        // 统计门店总数
        Long totalStores = storeMapper.selectCount(null);
        
        // 统计营业中门店数
        LambdaQueryWrapper<Store> openWrapper = new LambdaQueryWrapper<>();
        openWrapper.eq(Store::getStatus, 1);
        Long openStores = storeMapper.selectCount(openWrapper);
        
        // 统计停业门店�?
        Long closedStores = totalStores - openStores;
        
        // 统计总房间数
        Long totalRooms = storeRoomMapper.selectCount(null);
        
        // 统计可用房间�?
        LambdaQueryWrapper<StoreRoom> availableWrapper = new LambdaQueryWrapper<>();
        availableWrapper.eq(StoreRoom::getStatus, 1);
        Long availableRooms = storeRoomMapper.selectCount(availableWrapper);
        
        // 计算平均评分
        List<Store> allStores = storeMapper.selectList(null);
        BigDecimal averageRating = allStores.stream()
                .map(Store::getRating)
                .filter(rating -> rating != null && rating.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(allStores.size()), 2, RoundingMode.HALF_UP);
        
        // 统计评价总数
        Long totalReviews = storeReviewMapper.selectCount(null);
        
        // 统计好评数量（假设评分>=4为好评）
        Long goodReviews = storeReviewMapper.countGoodReviews();
        
        return StoreStatisticsVO.builder()
                .totalStores(totalStores)
                .openStores(openStores)
                .closedStores(closedStores)
                .totalRooms(totalRooms)
                .availableRooms(availableRooms)
                .averageRating(averageRating)
                .totalReviews(totalReviews)
                .goodReviews(goodReviews)
                .build();
    }

    @Override
    public List<StoreVO> listAll() {
        List<Store> stores = storeMapper.selectList(null);
        return stores.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门门店（添加Redis缓存）
     */
    @Override
    // @Cacheable(value = "hot:stores", unless = "#result == null || #result.isEmpty()")
    public List<StoreVO> getHotStores() {
        try {
            LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Store::getStatus, 1);
            wrapper.orderByDesc(Store::getCreateTime);
            wrapper.last("LIMIT 10");
            List<Store> stores = storeMapper.selectList(wrapper);
            return stores.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // 打印异常日志
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * 获取推荐门店
     */
    @Override
    public List<StoreVO> getRecommendedStores() {
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getStatus, 1);
        wrapper.orderByDesc(Store::getCreateTime);
        wrapper.last("LIMIT 10");
        List<Store> stores = storeMapper.selectList(wrapper);
        return stores.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取附近门店（基于地理位置）
     * 使用Haversine公式计算距离
     */
    @Override
    public List<StoreVO> getNearbyStores(Double latitude, Double longitude, Integer limit) {
        // 获取所有营业中的门�?
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getStatus, 1);
        List<Store> allStores = storeMapper.selectList(wrapper);
        
        // 计算每个门店的距离并排序
        List<StoreWithDistance> storesWithDistance = allStores.stream()
            .filter(store -> store.getLatitude() != null && store.getLongitude() != null)
            .map(store -> {
                double distance = calculateDistance(
                    latitude, longitude,
                    store.getLatitude().doubleValue(),
                    store.getLongitude().doubleValue()
                );
                return new StoreWithDistance(store, distance);
            })
            .sorted(Comparator.comparingDouble(StoreWithDistance::getDistance))
            .limit(limit != null ? limit : 10)
            .collect(Collectors.toList());
        
        // 转换为VO
        return storesWithDistance.stream()
            .map(swd -> {
                StoreVO vo = convertToVO(swd.getStore());
                // 设置距离信息（单位：公里，保�?位小数）
                vo.setDistance(Math.round(swd.getDistance() * 100.0) / 100.0);
                return vo;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 门店和距离的包装�?
     */
    private static class StoreWithDistance {
        private final Store store;
        private final double distance;
        
        public StoreWithDistance(Store store, double distance) {
            this.store = store;
            this.distance = distance;
        }
        
        public Store getStore() {
            return store;
        }
        
        public double getDistance() {
            return distance;
        }
    }

    /**
     * 实体转VO
     */
    private StoreVO convertToVO(Store store) {
        if (store == null) {
            return null;
        }
        StoreVO storeVO = new StoreVO();
        BeanUtils.copyProperties(store, storeVO);
        // 手动转换LocalTime为String
        if (store.getOpenTime() != null) {
            storeVO.setOpenTime(store.getOpenTime().toString());
        }
        if (store.getCloseTime() != null) {
            storeVO.setCloseTime(store.getCloseTime().toString());
        }
        return storeVO;
    }
    
    /**
     * 门店账号登录
     */
    @Override
    public StoreLoginVO storeLogin(StoreLoginDTO storeLoginDTO) {
        // 根据登录账号查询门店
        LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Store::getLoginAccount, storeLoginDTO.getLoginAccount())
               .eq(Store::getIsDeleted, 0);
        Store store = storeMapper.selectOne(wrapper);
        
        if (store == null) {
            throw new RuntimeException("门店账号不存在");
        }
        
        // 检查门店状态
        if (store.getStatus() != 1) {
            throw new RuntimeException("该门店已停业，无法登录");
        }
        
        // 验证密码
        String password = DigestUtils.md5DigestAsHex(storeLoginDTO.getPassword().getBytes());
        if (!password.equals(store.getLoginPassword())) {
            throw new PasswordErrorException("密码错误");
        }
        
        // 生成JWT令牌
        Map<String, Object> claims = new HashMap<>();
        // 使用门店ID作为用户ID（负值区分普通用户）
        claims.put(JwtClaimsConstant.USER_ID, -store.getId());
        claims.put(JwtClaimsConstant.STORE_ID, store.getId());
        claims.put("storeName", store.getName());
        claims.put(JwtClaimsConstant.ROLE, "STORE_ADMIN");
        
        String token = JwtUtil.createJWT(
                jwtProperties.getAdminSecretKey(),
                jwtProperties.getAdminTtl(),
                claims
        );
        
        log.info("门店登录成功: storeId={}, storeName={}", store.getId(), store.getName());
        
        // 构建返回对象
        return StoreLoginVO.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .loginAccount(store.getLoginAccount())
                .address(store.getAddress())
                .phone(store.getPhone())
                .images(store.getImages())
                .token(token)
                .role("STORE_ADMIN")
                .build();
    }
    
    /**
     * 更新门店登录密码
     */
    @Override
    public void updateLoginPassword(Long storeId, String oldPassword, String newPassword) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("门店不存在");
        }
        
        // 验证旧密码
        String oldPwd = DigestUtils.md5DigestAsHex(oldPassword.getBytes());
        if (!oldPwd.equals(store.getLoginPassword())) {
            throw new PasswordErrorException("原密码错误");
        }
        
        // 更新新密码
        String newPwd = DigestUtils.md5DigestAsHex(newPassword.getBytes());
        store.setLoginPassword(newPwd);
        storeMapper.updateById(store);
        
        log.info("门店密码修改成功: storeId={}", storeId);
    }
    
    /**
     * 更新门店账号信息（超级管理员使用）
     */
    @Override
    public void updateStoreAccount(Long storeId, String loginAccount, String loginPassword) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("门店不存在");
        }
        
        // 检查账号是否已被其他门店使用
        if (loginAccount != null && !loginAccount.isEmpty()) {
            LambdaQueryWrapper<Store> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Store::getLoginAccount, loginAccount)
                   .ne(Store::getId, storeId)
                   .eq(Store::getIsDeleted, 0);
            Store existStore = storeMapper.selectOne(wrapper);
            if (existStore != null) {
                throw new RuntimeException("该登录账号已被其他门店使用");
            }
            store.setLoginAccount(loginAccount);
        }
        
        // 如果设置了新密码，则更新密码
        if (loginPassword != null && !loginPassword.isEmpty()) {
            String newPwd = DigestUtils.md5DigestAsHex(loginPassword.getBytes());
            store.setLoginPassword(newPwd);
        }
        
        storeMapper.updateById(store);
        log.info("门店账号信息更新成功: storeId={}, loginAccount={}", storeId, loginAccount);
    }
    
    /**
     * 重置门店密码为默认值（123456）
     */
    @Override
    public void resetStorePassword(Long storeId) {
        Store store = storeMapper.selectById(storeId);
        if (store == null) {
            throw new RuntimeException("门店不存在");
        }
        
        // 默认密码: 123456
        String defaultPassword = DigestUtils.md5DigestAsHex("123456".getBytes());
        store.setLoginPassword(defaultPassword);
        storeMapper.updateById(store);
        
        log.info("门店密码已重置为默认值: storeId={}", storeId);
    }
}
