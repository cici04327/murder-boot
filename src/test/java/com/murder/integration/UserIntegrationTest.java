package com.murder.integration;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.murder.dto.UserLoginDTO;
import com.murder.dto.UserRegisterDTO;
import com.murder.entity.User;
import com.murder.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.DigestUtils;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户模块集成测试
 */
@DisplayName("用户模块集成测试")
class UserIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UserMapper userMapper;

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
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data").value("注册成功"));

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, "newuser");
            User saved = userMapper.selectOne(wrapper);

            assertNotNull(saved);
            assertEquals("新用户", saved.getNickname());
            assertEquals("13900139000", saved.getPhone());
            assertEquals("USER", saved.getRole());
            assertEquals(1, saved.getStatus());
            assertEquals(1, saved.getMemberLevel());
            assertEquals(0, saved.getPoints());
            assertEquals(DigestUtils.md5DigestAsHex("123456".getBytes()), saved.getPassword());
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
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value("用户已存在"));
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
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value("用户名不能为空"));

            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getUsername, "");
            assertEquals(0L, userMapper.selectCount(wrapper));
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
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                    .andExpect(jsonPath("$.data.phone").value("13800138000"))
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
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value("用户不存在"));
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
                    .andExpect(jsonPath("$.code").value(not(200)))
                    .andExpect(jsonPath("$.msg").value("密码错误"));
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
                    .andExpect(jsonPath("$.data.total").value(2))
                    .andExpect(jsonPath("$.data.records", hasSize(2)));
        }

        @Test
        @DisplayName("根据ID查询用户")
        void getById_Success() throws Exception {
            mockMvc.perform(get("/api/user/1")
                            .header("token", testUserToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.username").value("testuser"))
                    .andExpect(jsonPath("$.data.nickname").value("测试用户"))
                    .andExpect(jsonPath("$.data.role").value("USER"))
                    .andExpect(jsonPath("$.data.status").value(1));
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
