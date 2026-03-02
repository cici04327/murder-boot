package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.entity.Script;

/**
 * 剧本收藏服务接口
 */
public interface ScriptFavoriteService {
    
    /**
     * 收藏剧本
     */
    void favoriteScript(Long userId, Long scriptId);
    
    /**
     * 取消收藏剧本
     */
    void unfavoriteScript(Long userId, Long scriptId);
    
    /**
     * 检查是否已收藏
     */
    boolean isFavorited(Long userId, Long scriptId);
    
    /**
     * 获取用户收藏的剧本列�?
     */
    PageResult<Script> getUserFavorites(Long userId, Integer page, Integer pageSize);
    
    /**
     * 获取用户收藏数量
     */
    Integer getFavoriteCount(Long userId);
}

