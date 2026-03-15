package com.murder.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.murder.common.context.BaseContext;
import com.murder.common.result.PageResult;
import com.murder.entity.Store;
import com.murder.entity.StoreReview;
import com.murder.entity.User;
import com.murder.mapper.StoreMapper;
import com.murder.mapper.StoreReviewMapper;
import com.murder.mapper.UserMapper;
import com.murder.service.impl.StoreReviewServiceImpl;
import com.murder.vo.StoreReviewVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 门店评价服务测试类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class StoreReviewServiceTest {

    @Mock
    private StoreReviewMapper reviewMapper;

    @Mock
    private StoreMapper storeMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private StoreReviewServiceImpl storeReviewService;

    @Test
    public void testAddReviewSuccess() {
        // Arrange
        StoreReview review = StoreReview.builder()
                .storeId(1L)
                .rating(5)
                .content("很好的体验")
                .build();

        when(reviewMapper.insert(any(StoreReview.class))).thenReturn(1);

        // Act
        storeReviewService.add(review);

        // Assert
        verify(reviewMapper, times(1)).insert(any(StoreReview.class));
    }

    @Test
    public void testPageQueryWithStoreId() {
        // Arrange
        Long storeId = 1L;
        Integer page = 1;
        Integer pageSize = 10;

        StoreReview review1 = StoreReview.builder()
                .id(1L)
                .storeId(storeId)
                .userId(1L)
                .rating(5)
                .content("评价1")
                .isAnonymous(0)
                .createTime(LocalDateTime.now())
                .build();

        StoreReview review2 = StoreReview.builder()
                .id(2L)
                .storeId(storeId)
                .userId(2L)
                .rating(4)
                .content("评价2")
                .isAnonymous(0)
                .createTime(LocalDateTime.now())
                .build();

        Store store = Store.builder()
                .id(storeId)
                .name("测试门店")
                .build();

        User user1 = User.builder()
                .id(1L)
                .nickname("用户1")
                .avatar("avatar1.jpg")
                .build();

        User user2 = User.builder()
                .id(2L)
                .nickname("用户2")
                .avatar("avatar2.jpg")
                .build();

        List<StoreReview> reviewList = new ArrayList<>();
        reviewList.add(review1);
        reviewList.add(review2);

        when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<StoreReview> pageParam = invocation.getArgument(0);
                    pageParam.setRecords(reviewList);
                    return pageParam;
                });
        when(storeMapper.selectById(storeId)).thenReturn(store);
        when(userMapper.selectById(1L)).thenReturn(user1);
        when(userMapper.selectById(2L)).thenReturn(user2);

        // Act
        PageResult<StoreReviewVO> result = storeReviewService.pageQuery(storeId, page, pageSize);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getTotal());
        assertEquals(2, result.getRecords().size());
        assertEquals("测试门店", result.getRecords().get(0).getStoreName());
        assertEquals("用户1", result.getRecords().get(0).getUserNickname());
    }

    @Test
    public void testPageQueryEmpty() {
        // Arrange
        Long storeId = 1L;
        Integer page = 1;
        Integer pageSize = 10;

        when(reviewMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(reviewMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenAnswer(invocation -> {
                    Page<StoreReview> pageParam = invocation.getArgument(0);
                    pageParam.setRecords(new ArrayList<>());
                    return pageParam;
                });

        // Act
        PageResult<StoreReviewVO> result = storeReviewService.pageQuery(storeId, page, pageSize);

        // Assert
        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(0, result.getRecords().size());
    }

    @Test
    public void testGetByIdSuccess() {
        // Arrange
        Long reviewId = 1L;
        StoreReview review = StoreReview.builder()
                .id(reviewId)
                .storeId(1L)
                .userId(1L)
                .rating(5)
                .content("很好")
                .isAnonymous(0)
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("测试门店")
                .build();

        User user = User.builder()
                .id(1L)
                .nickname("测试用户")
                .avatar("avatar.jpg")
                .build();

        when(reviewMapper.selectById(reviewId)).thenReturn(review);
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(userMapper.selectById(1L)).thenReturn(user);

        // Act
        StoreReviewVO result = storeReviewService.getById(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals("测试门店", result.getStoreName());
        assertEquals("测试用户", result.getUserNickname());
    }

    @Test
    public void testReplySuccess() {
        // Arrange
        Long reviewId = 1L;
        String replyContent = "感谢您的好评，欢迎再次光临！";

        when(reviewMapper.updateById(any(StoreReview.class))).thenReturn(1);

        // Act
        storeReviewService.reply(reviewId, replyContent);

        // Assert
        verify(reviewMapper, times(1)).updateById(any(StoreReview.class));
    }

    @Test
    public void testDeleteSuccess() {
        // Arrange
        Long reviewId = 1L;

        when(reviewMapper.deleteById(reviewId)).thenReturn(1);

        // Act
        storeReviewService.delete(reviewId);

        // Assert
        verify(reviewMapper, times(1)).deleteById(reviewId);
    }

    @Test
    public void testGetByIdAnonymousUser() {
        // Arrange
        Long reviewId = 1L;
        StoreReview review = StoreReview.builder()
                .id(reviewId)
                .storeId(1L)
                .userId(1L)
                .rating(5)
                .content("匿名评价")
                .isAnonymous(1)
                .build();

        Store store = Store.builder()
                .id(1L)
                .name("测试门店")
                .build();

        User user = User.builder()
                .id(1L)
                .nickname("用户昵称")
                .avatar("avatar.jpg")
                .build();

        when(reviewMapper.selectById(reviewId)).thenReturn(review);
        when(storeMapper.selectById(1L)).thenReturn(store);
        when(userMapper.selectById(1L)).thenReturn(user);

        // Act
        StoreReviewVO result = storeReviewService.getById(reviewId);

        // Assert
        assertNotNull(result);
        assertEquals("匿名用户", result.getUserNickname());
        assertNull(result.getUserAvatar());
    }

    @Test
    public void testAddReviewWithBaseContext() {
        // Arrange
        StoreReview review = StoreReview.builder()
                .storeId(1L)
                .rating(5)
                .content("很好的体验")
                .build();

        when(reviewMapper.insert(any(StoreReview.class))).thenReturn(1);

        // Act
        storeReviewService.add(review);

        // Assert
        verify(reviewMapper, times(1)).insert(any(StoreReview.class));
    }
}
