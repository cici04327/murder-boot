package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.entity.Store;
import com.murder.entity.StoreReview;
import com.murder.vo.StoreReviewVO;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.StoreReviewMapper;
import com.murder.entity.User;
import com.murder.mapper.UserMapper;
import com.murder.service.StoreReviewService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 门店评价服务实现�?
 */
@Service
public class StoreReviewServiceImpl implements StoreReviewService {

    @Autowired
    private StoreReviewMapper reviewMapper;
    
    @Autowired
    private StoreMapper storeMapper;
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<StoreReviewVO> pageQuery(Long storeId, Integer page, Integer pageSize) {
        Page<StoreReview> pageInfo = new Page<>(page, pageSize);
        
        LambdaQueryWrapper<StoreReview> wrapper = new LambdaQueryWrapper<>();
        if (storeId != null) {
            wrapper.eq(StoreReview::getStoreId, storeId);
        }
        wrapper.orderByDesc(StoreReview::getCreateTime);
        
        // 手动查询总数
        Long total = reviewMapper.selectCount(wrapper);
        
        reviewMapper.selectPage(pageInfo, wrapper);
        
        List<StoreReviewVO> voList = pageInfo.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        
        // 使用手动查询�?total �?
        return new PageResult<>(total, voList);
    }

    @Override
    public StoreReviewVO getById(Long id) {
        StoreReview review = reviewMapper.selectById(id);
        return convertToVO(review);
    }

    @Override
    public void add(StoreReview review) {
        // 从ThreadLocal获取当前登录用户ID
        Long currentUserId = BaseContext.getCurrentId();
        if (currentUserId != null) {
            review.setUserId(currentUserId);
        }
        reviewMapper.insert(review);
    }

    @Override
    public void reply(Long id, String reply) {
        StoreReview review = new StoreReview();
        review.setId(id);
        review.setReply(reply);
        reviewMapper.updateById(review);
    }

    @Override
    public void delete(Long id) {
        reviewMapper.deleteById(id);
    }

    private StoreReviewVO convertToVO(StoreReview review) {
        if (review == null) {
            return null;
        }
        StoreReviewVO vo = new StoreReviewVO();
        BeanUtils.copyProperties(review, vo);
        
        // 查询门店名称
        if (review.getStoreId() != null) {
            Store store = storeMapper.selectById(review.getStoreId());
            if (store != null) {
                vo.setStoreName(store.getName());
            }
        }
        
        // 查询用户昵称：单体模式下直接查本地用户表，避免遗留微服务端口(8081)导致网络错误
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
