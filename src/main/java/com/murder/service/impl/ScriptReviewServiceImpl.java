package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.dto.ScriptReviewDTO;
import com.murder.entity.Script;
import com.murder.entity.ScriptReview;
import com.murder.vo.ScriptReviewVO;
import com.murder.mapper.ScriptMapper;
import com.murder.mapper.ScriptReviewMapper;
import com.murder.mapper.UserMapper;
import com.murder.entity.User;
import com.murder.service.ScriptReviewService;
import com.murder.service.DmService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 剧本评价服务实现类
 */
@Slf4j
@Service
public class ScriptReviewServiceImpl implements ScriptReviewService {

    @Autowired
    private ScriptReviewMapper reviewMapper;
    
    @Autowired
    private ScriptMapper scriptMapper;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired(required = false)
    private DmService dmService;

    @Override
    @Transactional
    public void add(ScriptReviewDTO reviewDTO) {
        ScriptReview review = new ScriptReview();
        BeanUtils.copyProperties(reviewDTO, review);
        
        // 设置用户ID
        Long userId = BaseContext.getCurrentId();
        if (userId != null) {
            review.setUserId(userId);
        }
        
        // 保存评价
        reviewMapper.insert(review);
        
        // 更新剧本平均评分
        updateScriptRating(reviewDTO.getScriptId());
        
        // 如果ScriptReviewDTO中包含dmId，则刷新DM评分
        if (reviewDTO.getDmId() != null && dmService != null) {
            try {
                dmService.refreshRating(reviewDTO.getDmId());
            } catch (Exception e) {
                // 刷新DM评分失败不影响主流程
                log.warn("刷新DM评分失败: dmId={}", reviewDTO.getDmId(), e);
            }
        }
    }

    @Override
    public PageResult<ScriptReviewVO> pageQuery(Long scriptId, Integer page, Integer pageSize) {
        // 先查询总数
        LambdaQueryWrapper<ScriptReview> countWrapper = new LambdaQueryWrapper<>();
        if (scriptId != null) {
            countWrapper.eq(ScriptReview::getScriptId, scriptId);
        }
        Long total = reviewMapper.selectCount(countWrapper);
        
        // 再查询分页数�?
        Page<ScriptReview> pageInfo = new Page<>(page, pageSize);
        LambdaQueryWrapper<ScriptReview> wrapper = new LambdaQueryWrapper<>();
        if (scriptId != null) {
            wrapper.eq(ScriptReview::getScriptId, scriptId);
        }
        wrapper.orderByDesc(ScriptReview::getCreateTime);
        
        reviewMapper.selectPage(pageInfo, wrapper);
        
        List<ScriptReviewVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        return new PageResult<>(total, voList);
    }

    @Override
    public ScriptReviewVO getById(Long id) {
        ScriptReview review = reviewMapper.selectById(id);
        return convertToVO(review);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ScriptReview review = reviewMapper.selectById(id);
        if (review != null) {
            reviewMapper.deleteById(id);
            // 更新剧本平均评分
            updateScriptRating(review.getScriptId());
        }
    }

    @Override
    public ScriptReview getByReservationId(Long reservationId) {
        LambdaQueryWrapper<ScriptReview> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScriptReview::getReservationId, reservationId);
        return reviewMapper.selectOne(wrapper);
    }

    /**
     * 更新剧本评分
     */
    private void updateScriptRating(Long scriptId) {
        BigDecimal avgRating = reviewMapper.calculateAverageRating(scriptId);
        if (avgRating != null) {
            Script script = new Script();
            script.setId(scriptId);
            script.setRating(avgRating.setScale(2, RoundingMode.HALF_UP));
            scriptMapper.updateById(script);
        }
    }

    /**
     * 转换为VO
     */
    private ScriptReviewVO convertToVO(ScriptReview review) {
        if (review == null) {
            return null;
        }
        ScriptReviewVO vo = new ScriptReviewVO();
        BeanUtils.copyProperties(review, vo);
        
        // 查询剧本名称
        if (review.getScriptId() != null) {
            Script script = scriptMapper.selectById(review.getScriptId());
            if (script != null) {
                vo.setScriptName(script.getName());
            }
        }
        
        // 查询用户昵称：直接查本地用户表
        if (review.getUserId() != null) {
            try {
                User user = userMapper.selectById(review.getUserId());
                if (user != null && user.getNickname() != null && !user.getNickname().isBlank()) {
                    vo.setUserNickname(user.getNickname());
                } else {
                    vo.setUserNickname("用户" + review.getUserId());
                }
            } catch (Exception e) {
                vo.setUserNickname("用户" + review.getUserId());
            }
        } else {
            vo.setUserNickname("匿名用户");
        }
        
        return vo;
    }
}
