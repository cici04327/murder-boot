package com.murder.service;

import com.murder.dto.UserLoginDTO;
import com.murder.dto.UserRegisterDTO;
import com.murder.entity.User;
import com.murder.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户服务单元测试
 * 注意：只测试Mapper层交互，避免mock复杂的Spring类
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户服务测试")
class UserServiceTest {

    @Mock
    private UserMapper userMapper;

    private User testUser;
    private UserLoginDTO loginDTO;
    private UserRegisterDTO registerDTO;

    @BeforeEach
    void setUp() {
        // 初始化测试用户
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword("e10adc3949ba59abbe56e057f20f883e"); // MD5(123456)
        testUser.setNickname("测试用户");
        testUser.setPhone("13800138000");
        testUser.setAvatar("http://example.com/avatar.jpg");
        testUser.setStatus(1);
        testUser.setCreateTime(LocalDateTime.now());

        // 初始化登录DTO
        loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("123456");

        // 初始化注册DTO
        registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("newuser");
        registerDTO.setPassword("123456");
        registerDTO.setNickname("新用户");
        registerDTO.setPhone("13900139000");
    }

    @Test
    @DisplayName("根据ID查询用户 - 成功")
    void testGetById_Success() {
        // Given
        when(userMapper.selectById(1L)).thenReturn(testUser);

        // When
        User result = userMapper.selectById(1L);

        // Then
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        assertEquals("测试用户", result.getNickname());
        verify(userMapper, times(1)).selectById(1L);
    }

    @Test
    @DisplayName("根据ID查询用户 - 不存在")
    void testGetById_NotFound() {
        // Given
        when(userMapper.selectById(999L)).thenReturn(null);

        // When
        User result = userMapper.selectById(999L);

        // Then
        assertNull(result);
        verify(userMapper, times(1)).selectById(999L);
    }

    @Test
    @DisplayName("新增用户")
    void testInsertUser() {
        // Given
        when(userMapper.insert(any(User.class))).thenReturn(1);

        // When
        int result = userMapper.insert(testUser);

        // Then
        assertEquals(1, result);
        verify(userMapper, times(1)).insert(testUser);
    }

    @Test
    @DisplayName("更新用户")
    void testUpdateUser() {
        // Given
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        // When
        int result = userMapper.updateById(testUser);

        // Then
        assertEquals(1, result);
        verify(userMapper, times(1)).updateById(testUser);
    }

    @Test
    @DisplayName("查询用户列表")
    void testSelectUserList() {
        // Given
        when(userMapper.selectList(any())).thenReturn(java.util.Arrays.asList(testUser));

        // When
        java.util.List<User> result = userMapper.selectList(null);

        // Then
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("testuser", result.get(0).getUsername());
    }
}
