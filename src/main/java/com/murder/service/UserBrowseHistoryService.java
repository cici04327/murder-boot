package com.murder.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.murder.entity.UserBrowseHistory;
import com.murder.vo.BrowseHistoryVO;

/**
 * 用户浏览历史服务接口
 */
public interface UserBrowseHistoryService extends IService<UserBrowseHistory> {

    /**
     * 分页查询用户浏览历史
     * @param userId 用户ID
     * @param page 页码
     * @param pageSize 每页大小
     * @param days 天数筛选（null表示全部）
     * @param targetType 目标类型筛选（null表示全部）
     * @return 分页结果
     */
    Page<BrowseHistoryVO> pageHistory(Long userId, Integer page, Integer pageSize, Integer days, Integer targetType);

    /**
     * 记录浏览历史
     * @param userId 用户ID
     * @param targetType 目标类型
     * @param targetId 目标ID
     * @param duration 浏览时长
     */
    void recordHistory(Long userId, Integer targetType, Long targetId, Integer duration);

    /**
     * 删除单条浏览记录
     * @param userId 用户ID
     * @param historyId 记录ID
     * @return 是否删除成功
     */
    boolean deleteHistory(Long userId, Long historyId);

    /**
     * 清空用户所有浏览历史
     * @param userId 用户ID
     * @return 删除的记录数
     */
    int clearHistory(Long userId);

    /**
     * 获取用户浏览历史统计
     * @param userId 用户ID
     * @return 统计信息
     */
    BrowseHistoryStatsVO getHistoryStats(Long userId);

    /**
     * 浏览历史统计VO（内部类）
     */
    class BrowseHistoryStatsVO {
        private Long totalCount;        // 总浏览数
        private Long scriptCount;       // 剧本浏览数
        private Long storeCount;        // 门店浏览数
        private Long todayCount;        // 今日浏览数
        private Long weekCount;         // 本周浏览数

        public Long getTotalCount() { return totalCount; }
        public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
        public Long getScriptCount() { return scriptCount; }
        public void setScriptCount(Long scriptCount) { this.scriptCount = scriptCount; }
        public Long getStoreCount() { return storeCount; }
        public void setStoreCount(Long storeCount) { this.storeCount = storeCount; }
        public Long getTodayCount() { return todayCount; }
        public void setTodayCount(Long todayCount) { this.todayCount = todayCount; }
        public Long getWeekCount() { return weekCount; }
        public void setWeekCount(Long weekCount) { this.weekCount = weekCount; }
    }
}
