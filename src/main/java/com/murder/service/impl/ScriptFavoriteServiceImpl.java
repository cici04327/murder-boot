package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.exception.BaseException;
import com.murder.common.result.PageResult;
import com.murder.entity.Script;
import com.murder.entity.ScriptFavorite;
import com.murder.mapper.ScriptFavoriteMapper;
import com.murder.mapper.ScriptMapper;
import com.murder.service.ScriptFavoriteService;
import com.murder.service.UserPointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 剧本收藏服务实现?
 */
@Service
@Slf4j
public class ScriptFavoriteServiceImpl implements ScriptFavoriteService {
    
    @Autowired
    private ScriptFavoriteMapper scriptFavoriteMapper;
    
    @Autowired
    private ScriptMapper scriptMapper;
    
    
    @Autowired(required = false)
    private UserPointsService userPointsService;
    
    /**
     * 收藏剧本
     */
    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"script:favorite", "script:favorite:list"}, allEntries = true)
    public void favoriteScript(Long userId, Long scriptId) {
        // 检查是否已收藏
        LambdaQueryWrapper<ScriptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptFavorite::getUserId, userId)
               .eq(ScriptFavorite::getScriptId, scriptId);
        
        Long count = scriptFavoriteMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BaseException("已收藏该剧本");
        }
        
        // 创建收藏记录
        ScriptFavorite favorite = ScriptFavorite.builder()
                .userId(userId)
                .scriptId(scriptId)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        
        scriptFavoriteMapper.insert(favorite);
        
        // 获取用户当前的收藏数?
        Integer favoriteCount = getFavoriteCount(userId);
        log.info("用户{}收藏剧本{}，当前收藏数? {}", userId, scriptId, favoriteCount);
        
        // 每收?个剧本奖?0积分
        if (favoriteCount % 5 == 0) {
            try {
                if (userPointsService != null) {
                    userPointsService.addPoints(userId, 20, "收藏剧本达到" + favoriteCount + "个");
                    log.info("用户{}收藏剧本达到{}个，获得20积分奖励", userId, favoriteCount);
                }
            } catch (Exception e) {
                log.error("调用积分服务失败", e);
                // 积分服务失败不影响收藏功?
            }
        }
    }
    
    /**
     * 取消收藏剧本
     */
    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"script:favorite", "script:favorite:list"}, allEntries = true)
    public void unfavoriteScript(Long userId, Long scriptId) {
        LambdaQueryWrapper<ScriptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptFavorite::getUserId, userId)
               .eq(ScriptFavorite::getScriptId, scriptId);
        
        scriptFavoriteMapper.delete(wrapper);
    }
    
    /**
     * 检查是否已收藏（添加缓存）
     */
    @Override
    @org.springframework.cache.annotation.Cacheable(value = "script:favorite", key = "#userId + '_' + #scriptId")
    public boolean isFavorited(Long userId, Long scriptId) {
        LambdaQueryWrapper<ScriptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptFavorite::getUserId, userId)
               .eq(ScriptFavorite::getScriptId, scriptId);
        
        Long count = scriptFavoriteMapper.selectCount(wrapper);
        return count > 0;
    }
    
    /**
     * 获取用户收藏的剧本列?
     */
    @Override
    public PageResult<Script> getUserFavorites(Long userId, Integer page, Integer pageSize) {
        // 查询用户收藏的剧本ID列表
        LambdaQueryWrapper<ScriptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptFavorite::getUserId, userId)
               .orderByDesc(ScriptFavorite::getCreateTime);
        
        Page<ScriptFavorite> pageInfo = new Page<>(page, pageSize);
        Page<ScriptFavorite> favoritePage = scriptFavoriteMapper.selectPage(pageInfo, wrapper);
        
        List<Long> scriptIds = favoritePage.getRecords().stream()
                .map(ScriptFavorite::getScriptId)
                .collect(Collectors.toList());
        
        // 查询剧本详情
        List<Script> scripts;
        if (scriptIds.isEmpty()) {
            scripts = List.of();
        } else {
            LambdaQueryWrapper<Script> scriptWrapper = new LambdaQueryWrapper<>();
            scriptWrapper.in(Script::getId, scriptIds);
            List<Script> rawScripts = scriptMapper.selectList(scriptWrapper);
            // 按收藏顺序排列，过滤掉已被删除的剧本
            scripts = scriptIds.stream()
                    .map(id -> rawScripts.stream()
                            .filter(s -> s.getId().equals(id))
                            .findFirst()
                            .orElse(null))
                    .filter(s -> s != null)
                    .collect(Collectors.toList());
        }

        // total 以实际存在的剧本数为准（过滤掉已删除的剧本后修正）
        long total = Math.max(favoritePage.getTotal() - (scriptIds.size() - scripts.size()), scripts.size());
        log.info("查询用户{}的收藏列表，DB总数:{}, 实际返回:{}, 修正后total:{}", userId, favoritePage.getTotal(), scripts.size(), total);
        
        return new PageResult<>(total, scripts);
    }
    
    /**
     * 获取用户收藏数量
     */
    @Override
    public Integer getFavoriteCount(Long userId) {
        LambdaQueryWrapper<ScriptFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptFavorite::getUserId, userId);
        
        Long count = scriptFavoriteMapper.selectCount(wrapper);
        return count.intValue();
    }
}

