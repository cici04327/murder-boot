package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.result.PageResult;
import com.murder.dto.DmDTO;
import com.murder.entity.Dm;
import com.murder.entity.Review;
import com.murder.entity.Store;
import com.murder.mapper.DmMapper;
import com.murder.mapper.ReviewMapper;
import com.murder.mapper.StoreMapper;
import com.murder.service.DmService;
import com.murder.vo.DmVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DM 服务实现
 */
@Slf4j
@Service
public class DmServiceImpl implements DmService {

    @Autowired
    private DmMapper dmMapper;

    @Autowired
    private StoreMapper storeMapper;

    @Autowired
    private ReviewMapper reviewMapper;

    // ------------------------------------------------------------------ 查询

    @Override
    public PageResult<DmVO> pageQuery(Long storeId, Integer status, Integer page, Integer pageSize) {
        LambdaQueryWrapper<Dm> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(Dm::getStoreId, storeId);
        }
        if (status != null) {
            wrapper.eq(Dm::getStatus, status);
        }
        wrapper.orderByDesc(Dm::getCreateTime);

        long total = dmMapper.selectCount(wrapper);

        Page<Dm> pageInfo = new Page<>(page, pageSize);
        dmMapper.selectPage(pageInfo, wrapper);

        Map<Long, String> storeNameMap = buildStoreNameMap(pageInfo.getRecords());

        List<DmVO> voList = pageInfo.getRecords().stream()
                .map(dm -> convertToVO(dm, storeNameMap))
                .collect(Collectors.toList());

        return new PageResult<>(total, voList);
    }

    @Override
    public List<DmVO> listByStoreId(Long storeId) {
        LambdaQueryWrapper<Dm> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dm::getStoreId, storeId)
                .eq(Dm::getStatus, 1)
                .orderByDesc(Dm::getRating);
        List<Dm> list = dmMapper.selectList(wrapper);

        Map<Long, String> storeNameMap = buildStoreNameMap(list);
        return list.stream()
                .map(dm -> convertToVO(dm, storeNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public DmVO getById(Long id) {
        Dm dm = dmMapper.selectById(id);
        if (dm == null) {
            return null;
        }
        Map<Long, String> storeNameMap = buildStoreNameMap(Collections.singletonList(dm));
        return convertToVO(dm, storeNameMap);
    }

    // ------------------------------------------------------------------ 增改删

    @Override
    public void add(DmDTO dmDTO) {
        Dm dm = new Dm();
        BeanUtils.copyProperties(dmDTO, dm);
        dm.setId(null);                  // 防止前端传入 id
        dm.setRating(BigDecimal.ZERO);
        dm.setGameCount(0);
        dm.setStatus(dmDTO.getStatus() != null ? dmDTO.getStatus() : 1);
        dmMapper.insert(dm);
        log.info("新增 DM: storeId={}, name={}", dm.getStoreId(), dm.getName());
    }

    @Override
    public void update(DmDTO dmDTO) {
        Dm dm = new Dm();
        BeanUtils.copyProperties(dmDTO, dm);
        // rating / gameCount 不允许通过此接口修改，置空由 MyBatis-Plus 忽略
        dm.setRating(null);
        dm.setGameCount(null);
        dmMapper.updateById(dm);
        log.info("更新 DM: id={}", dm.getId());
    }

    @Override
    public void delete(Long id) {
        dmMapper.deleteById(id);
        log.info("删除 DM: id={}", id);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Dm dm = new Dm();
        dm.setId(id);
        dm.setStatus(status);
        dmMapper.updateById(dm);
        log.info("更新 DM 状态: id={}, status={}", id, status);
    }

    // ------------------------------------------------------------------ 评分刷新

    @Override
    public void refreshRating(Long dmId) {
        // 查询该 DM 的所有已通过评价，取 dmRating 均值
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Review::getDmId, dmId)
                .eq(Review::getStatus, 2)          // 已通过
                .isNotNull(Review::getDmRating);

        List<Review> reviews = reviewMapper.selectList(wrapper);
        if (reviews.isEmpty()) {
            return;
        }

        double avg = reviews.stream()
                .mapToInt(Review::getDmRating)
                .average()
                .orElse(0.0);

        BigDecimal newRating = BigDecimal.valueOf(avg).setScale(1, RoundingMode.HALF_UP);
        dmMapper.updateRatingAndGameCount(dmId, newRating, reviews.size());
        log.info("刷新 DM 评分: dmId={}, rating={}, gameCount={}", dmId, newRating, reviews.size());
    }

    // ------------------------------------------------------------------ 私有工具

    private Map<Long, String> buildStoreNameMap(List<Dm> list) {
        List<Long> storeIds = list.stream()
                .map(Dm::getStoreId)
                .distinct()
                .collect(Collectors.toList());
        if (storeIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Store> stores = storeMapper.selectBatchIds(storeIds);
        Map<Long, String> map = new HashMap<>();
        stores.forEach(s -> map.put(s.getId(), s.getName()));
        return map;
    }

    private DmVO convertToVO(Dm dm, Map<Long, String> storeNameMap) {
        DmVO vo = new DmVO();
        BeanUtils.copyProperties(dm, vo);
        vo.setStoreName(storeNameMap.get(dm.getStoreId()));
        vo.setStatusName(dm.getStatus() != null && dm.getStatus() == 1 ? "在职" : "离职");

        // 将逗号分隔的 styleTags 转换为列表
        if (dm.getStyleTags() != null && !dm.getStyleTags().isBlank()) {
            vo.setStyleTagList(Arrays.asList(dm.getStyleTags().split(",")));
        } else {
            vo.setStyleTagList(Collections.emptyList());
        }
        return vo;
    }
}
