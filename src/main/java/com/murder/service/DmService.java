package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.DmDTO;
import com.murder.entity.Dm;
import com.murder.vo.DmVO;

import java.util.List;

/**
 * DM 服务接口
 */
public interface DmService {

    /**
     * 分页查询（管理端）
     */
    PageResult<DmVO> pageQuery(Long storeId, Integer status, Integer page, Integer pageSize);

    /**
     * 查询门店下所有在职 DM（排期分配 / 用户端展示用）
     */
    List<DmVO> listByStoreId(Long storeId);

    /**
     * 根据 ID 查询
     */
    DmVO getById(Long id);

    /**
     * 新增
     */
    void add(DmDTO dmDTO);

    /**
     * 编辑
     */
    void update(DmDTO dmDTO);

    /**
     * 删除
     */
    void delete(Long id);

    /**
     * 更新状态（在职/离职）
     */
    void updateStatus(Long id, Integer status);

    /**
     * 刷新 DM 评分和场次（评价提交后调用）
     */
    void refreshRating(Long dmId);
}
