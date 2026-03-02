package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.murder.entity.Script;
import com.murder.entity.Store;
import com.murder.entity.UserBrowseHistory;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.UserBrowseHistoryMapper;
import com.murder.service.UserBrowseHistoryService;
import com.murder.vo.BrowseHistoryVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户浏览历史服务实现类
 */
@Slf4j
@Service
public class UserBrowseHistoryServiceImpl extends ServiceImpl<UserBrowseHistoryMapper, UserBrowseHistory> 
        implements UserBrowseHistoryService {

    @Autowired
    private ScriptMapper scriptMapper;

    @Autowired
    private StoreMapper storeMapper;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<BrowseHistoryVO> pageHistory(Long userId, Integer page, Integer pageSize, Integer days, Integer targetType) {
        // 构建查询条件（@TableLogic会自动处理isDeleted条件）
        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getUserId, userId);

        // 时间筛选
        if (days != null && days > 0) {
            LocalDateTime startTime = LocalDateTime.now().minusDays(days);
            wrapper.ge(UserBrowseHistory::getBrowseTime, startTime);
        }

        // 类型筛选
        if (targetType != null) {
            wrapper.eq(UserBrowseHistory::getTargetType, targetType);
        }

        // 按浏览时间倒序
        wrapper.orderByDesc(UserBrowseHistory::getBrowseTime);

        // 分页查询
        Page<UserBrowseHistory> historyPage = new Page<>(page, pageSize);
        Page<UserBrowseHistory> resultPage = this.page(historyPage, wrapper);

        // 转换为VO
        Page<BrowseHistoryVO> voPage = new Page<>(page, pageSize);
        voPage.setTotal(resultPage.getTotal());
        voPage.setPages(resultPage.getPages());

        List<BrowseHistoryVO> voList = convertToVOList(resultPage.getRecords());
        voPage.setRecords(voList);

        return voPage;
    }

    /**
     * 将浏览记录转换为VO列表
     */
    private List<BrowseHistoryVO> convertToVOList(List<UserBrowseHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            return new ArrayList<>();
        }

        // 按类型分组收集ID
        List<Long> scriptIds = histories.stream()
                .filter(h -> h.getTargetType() == 1)
                .map(UserBrowseHistory::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        List<Long> storeIds = histories.stream()
                .filter(h -> h.getTargetType() == 2)
                .map(UserBrowseHistory::getTargetId)
                .distinct()
                .collect(Collectors.toList());

        // 批量查询剧本信息
        Map<Long, Script> scriptMap = new java.util.HashMap<>();
        if (!scriptIds.isEmpty()) {
            List<Script> scripts = scriptMapper.selectBatchIds(scriptIds);
            scriptMap = scripts.stream().collect(Collectors.toMap(Script::getId, s -> s, (a, b) -> a));
        }

        // 批量查询门店信息
        Map<Long, Store> storeMap = new java.util.HashMap<>();
        if (!storeIds.isEmpty()) {
            List<Store> stores = storeMapper.selectBatchIds(storeIds);
            storeMap = stores.stream().collect(Collectors.toMap(Store::getId, s -> s, (a, b) -> a));
        }

        // 转换为VO
        List<BrowseHistoryVO> voList = new ArrayList<>();
        for (UserBrowseHistory history : histories) {
            BrowseHistoryVO vo = BrowseHistoryVO.builder()
                    .id(history.getId())
                    .userId(history.getUserId())
                    .targetType(history.getTargetType())
                    .targetId(history.getTargetId())
                    .browseTime(history.getBrowseTime())
                    .browseTimeStr(history.getBrowseTime() != null ? 
                            history.getBrowseTime().format(DATE_TIME_FORMATTER) : null)
                    .duration(history.getDuration())
                    .build();

            // 根据类型填充详情
            if (history.getTargetType() == 1) {
                // 剧本
                Script script = scriptMap.get(history.getTargetId());
                if (script != null) {
                    vo.setName(script.getName());
                    vo.setCover(script.getCover());
                    vo.setPrice(script.getPrice());
                    vo.setRating(script.getRating());
                    // 设置玩家人数
                    if (script.getPlayerCount() != null) {
                        vo.setPlayerCount(script.getPlayerCount() + "人");
                    }
                }
            } else if (history.getTargetType() == 2) {
                // 门店
                Store store = storeMap.get(history.getTargetId());
                if (store != null) {
                    vo.setName(store.getName());
                    // Store使用images字段，取第一张作为封面
                    String images = store.getImages();
                    if (images != null && !images.isEmpty()) {
                        vo.setCover(images.split(",")[0]);
                    }
                    vo.setAddress(store.getAddress());
                    vo.setRating(store.getRating());
                }
            }

            voList.add(vo);
        }

        return voList;
    }

    @Override
    @Transactional
    public void recordHistory(Long userId, Integer targetType, Long targetId, Integer duration) {
        // 检查是否已存在相同的浏览记录（同一用户、同一目标、5分钟内）
        // @TableLogic会自动处理isDeleted条件
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        LambdaQueryWrapper<UserBrowseHistory> existWrapper = new LambdaQueryWrapper<>();
        existWrapper.eq(UserBrowseHistory::getUserId, userId)
                   .eq(UserBrowseHistory::getTargetType, targetType)
                   .eq(UserBrowseHistory::getTargetId, targetId)
                   .ge(UserBrowseHistory::getBrowseTime, fiveMinutesAgo);

        UserBrowseHistory existingHistory = this.getOne(existWrapper);
        
        if (existingHistory != null) {
            // 更新浏览时间和时长
            existingHistory.setBrowseTime(LocalDateTime.now());
            if (duration != null && duration > 0) {
                existingHistory.setDuration(existingHistory.getDuration() + duration);
            }
            this.updateById(existingHistory);
        } else {
            // 创建新记录
            UserBrowseHistory history = UserBrowseHistory.builder()
                    .userId(userId)
                    .targetType(targetType)
                    .targetId(targetId)
                    .browseTime(LocalDateTime.now())
                    .duration(duration != null ? duration : 0)
                    .build();
            this.save(history);
        }
    }

    @Override
    @Transactional
    public boolean deleteHistory(Long userId, Long historyId) {
        LambdaUpdateWrapper<UserBrowseHistory> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserBrowseHistory::getId, historyId)
               .eq(UserBrowseHistory::getUserId, userId)
               .set(UserBrowseHistory::getIsDeleted, 1);
        return this.update(wrapper);
    }

    @Override
    @Transactional
    public int clearHistory(Long userId) {
        // 使用remove方法，@TableLogic会自动将删除转换为更新isDeleted字段
        LambdaQueryWrapper<UserBrowseHistory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserBrowseHistory::getUserId, userId);
        this.remove(wrapper);
        
        // 返回受影响的行数（这里简化处理）
        return 1;
    }

    @Override
    public BrowseHistoryStatsVO getHistoryStats(Long userId) {
        BrowseHistoryStatsVO stats = new BrowseHistoryStatsVO();

        // @TableLogic会自动处理isDeleted条件，无需手动添加

        // 总浏览数
        LambdaQueryWrapper<UserBrowseHistory> totalWrapper = new LambdaQueryWrapper<>();
        totalWrapper.eq(UserBrowseHistory::getUserId, userId);
        stats.setTotalCount(this.count(totalWrapper));

        // 剧本浏览数
        LambdaQueryWrapper<UserBrowseHistory> scriptWrapper = new LambdaQueryWrapper<>();
        scriptWrapper.eq(UserBrowseHistory::getUserId, userId)
                    .eq(UserBrowseHistory::getTargetType, 1);
        stats.setScriptCount(this.count(scriptWrapper));

        // 门店浏览数
        LambdaQueryWrapper<UserBrowseHistory> storeWrapper = new LambdaQueryWrapper<>();
        storeWrapper.eq(UserBrowseHistory::getUserId, userId)
                   .eq(UserBrowseHistory::getTargetType, 2);
        stats.setStoreCount(this.count(storeWrapper));

        // 今日浏览数
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LambdaQueryWrapper<UserBrowseHistory> todayWrapper = new LambdaQueryWrapper<>();
        todayWrapper.eq(UserBrowseHistory::getUserId, userId)
                   .ge(UserBrowseHistory::getBrowseTime, todayStart);
        stats.setTodayCount(this.count(todayWrapper));

        // 本周浏览数
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(WeekFields.of(Locale.CHINA).dayOfWeek(), 1);
        LocalDateTime weekStartTime = LocalDateTime.of(weekStart, LocalTime.MIN);
        LambdaQueryWrapper<UserBrowseHistory> weekWrapper = new LambdaQueryWrapper<>();
        weekWrapper.eq(UserBrowseHistory::getUserId, userId)
                  .ge(UserBrowseHistory::getBrowseTime, weekStartTime);
        stats.setWeekCount(this.count(weekWrapper));

        return stats;
    }
}
