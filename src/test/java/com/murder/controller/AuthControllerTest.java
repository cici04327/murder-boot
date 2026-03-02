package com.murder.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.dto.UserLoginDTO;
import com.murder.dto.UserRegisterDTO;
import com.murder.service.UserService;
import com.murder.vo.UserLoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户认证（登录/注册）单元测试（纯 Mockito，不依赖 Spring 容器）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("用户认证测试")
class AuthControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private UserLoginDTO loginDTO;
    private UserRegisterDTO registerDTO;
    private UserLoginVO loginVO;

    @BeforeEach
    void setUp() {
        // 初始化 MockMvc 和 ObjectMapper
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        objectMapper = new ObjectMapper();

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

        // 初始化登录响应
        loginVO = new UserLoginVO();
        loginVO.setId(1L);
        loginVO.setUsername("testuser");
        loginVO.setNickname("测试用户");
        loginVO.setToken("mock-jwt-token");
    }

    @Test
    @DisplayName("用户登录 - 成功")
    void testLogin_Success() throws Exception {
        // Given
        when(userService.login(any(UserLoginDTO.class))).thenReturn(loginVO);

        // When & Then
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.username").value("testuser"))
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    @DisplayName("用户登录 - 用户名为空")
    void testLogin_EmptyUsername() throws Exception {
        // Given
        loginDTO.setUsername("");
        when(userService.login(any(UserLoginDTO.class))).thenReturn(loginVO);

        // When & Then
        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("用户注册 - 成功")
    void testRegister_Success() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
