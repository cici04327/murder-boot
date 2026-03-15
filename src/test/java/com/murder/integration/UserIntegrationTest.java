package com.murder.integration;

import com.murder.dto.UserLoginDTO;
import com.murder.dto.UserRegisterDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户模块集成测试
 */
@DisplayName("用户模块集成测试")
class UserIntegrationTest extends BaseIntegrationTest {

    @Nested
    @DisplayName("用户注册测试")
    class RegisterTests {

        @Test
        @DisplayName("注册成功 - 正常注册新用户")
        void register_Success() throws Exception {
            UserRegisterDTO registerDTO = new UserRegisterDTO();
            registerDTO.setUsername("newuser");
            registerDTO.setPassword("123456");
            registerDTO.setNickname("新用户");
            registerDTO.setPhone("13900139000");

            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(registerDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("注册失败 - 用户名已存在")
        void register_UsernameExists() throws Exception {
            UserRegisterDTO registerDTO = new UserRegisterDTO();
            registerDTO.setUsername("testuser"); // 已存在的用户名
            registerDTO.setPassword("123456");
            registerDTO.setNickname("测试");
            registerDTO.setPhone("13900139001");

            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(registerDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)));
        }

        @Test
        @DisplayName("注册失败 - 用户名为空")
        void register_EmptyUsername() throws Exception {
            UserRegisterDTO registerDTO = new UserRegisterDTO();
            registerDTO.setUsername("");
            registerDTO.setPassword("123456");
            registerDTO.setNickname("测试");

            mockMvc.perform(post("/api/user/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(registerDTO)))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("用户登录测试")
    class LoginTests {

        @Test
        @DisplayName("登录成功 - 正确的用户名密码")
        void login_Success() throws Exception {
            UserLoginDTO loginDTO = new UserLoginDTO();
            loginDTO.setUsername("testuser");
            loginDTO.setPassword("123456");

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.token").exists());
        }

        @Test
        @DisplayName("登录失败 - 用户名不存在")
        void login_UserNotFound() throws Exception {
            UserLoginDTO loginDTO = new UserLoginDTO();
            loginDTO.setUsername("nonexistent");
            loginDTO.setPassword("123456");

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)));
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void login_WrongPassword() throws Exception {
            UserLoginDTO loginDTO = new UserLoginDTO();
            loginDTO.setUsername("testuser");
            loginDTO.setPassword("wrongpassword");

            mockMvc.perform(post("/api/user/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginDTO)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)));
        }
    }

    @Nested
    @DisplayName("用户信息查询测试")
    class UserQueryTests {

        @Test
        @DisplayName("分页查询用户列表")
        void pageQuery_Success() throws Exception {
            mockMvc.perform(get("/api/user/page")
                            .header("token", testUserToken)
                            .param("page", "1")
                            .param("pageSize", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(0)));
        }

        @Test
        @DisplayName("根据ID查询用户")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/user/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("testuser"));
        }

        @Test
        @DisplayName("查询不存在的用户")
        void getById_NotFound() throws Exception {
            mockMvc.perform(get("/api/user/99999")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }
}
