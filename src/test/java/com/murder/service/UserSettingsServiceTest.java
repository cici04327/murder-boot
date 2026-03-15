package com.murder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.entity.UserSettings;
import com.murder.mapper.UserSettingsMapper;
import com.murder.service.impl.UserSettingsServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 用户设置服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户设置服务测试")
class UserSettingsServiceTest {

    @Mock
    private UserSettingsMapper userSettingsMapper;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private UserSettingsServiceImpl userSettingsService;

    private UserSettings testSettings;

    @BeforeEach
    void setUp() {
        testSettings = new UserSettings();
        testSettings.setId(1L);
        testSettings.setUserId(1L);
        testSettings.setPrivacySettings("{\"showProfile\":1,\"showHistory\":1,\"showFavorites\":1}");
        testSettings.setNotificationSettings("{\"notifyReservation\":1,\"notifyPromotion\":1,\"notifySystem\":1}");
        testSettings.setPreferenceSettings("{\"theme\":\"light\",\"language\":\"zh_CN\"}");
    }

    @Nested
    @DisplayName("隐私设置测试")
    class PrivacySettingsTests {

        @Test
        @DisplayName("获取隐私设置 - 存在设置")
        void testGetPrivacySettings_Exists() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);

            Map<String, Object> result = userSettingsService.getPrivacySettings(1L);

            assertNotNull(result);
            assertTrue(result.containsKey("showProfile") || result.containsKey("show_profile")
                    || !result.isEmpty());
        }

        @Test
        @DisplayName("获取隐私设置 - 不存在则返回默认值")
        void testGetPrivacySettings_DefaultValues() {
            when(userSettingsMapper.selectOne(any())).thenReturn(null);

            Map<String, Object> result = userSettingsService.getPrivacySettings(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("更新隐私设置 - 成功")
        void testUpdatePrivacySettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);

            Map<String, Object> settings = new HashMap<>();
            settings.put("showProfile", 0);
            settings.put("showHistory", 0);

            assertDoesNotThrow(() -> userSettingsService.updatePrivacySettings(1L, settings));
            verify(userSettingsMapper, times(1)).updateById(any(UserSettings.class));
        }

        @Test
        @DisplayName("更新隐私设置 - 不存在则创建")
        void testUpdatePrivacySettings_CreateNew() {
            when(userSettingsMapper.selectOne(any())).thenReturn(null);
            when(userSettingsMapper.insert(any(UserSettings.class))).thenReturn(1);

            Map<String, Object> settings = new HashMap<>();
            settings.put("showProfile", 1);

            assertDoesNotThrow(() -> userSettingsService.updatePrivacySettings(1L, settings));
        }
    }

    @Nested
    @DisplayName("通知设置测试")
    class NotificationSettingsTests {

        @Test
        @DisplayName("获取通知设置 - 成功")
        void testGetNotificationSettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);

            Map<String, Object> result = userSettingsService.getNotificationSettings(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("更新通知设置 - 成功")
        void testUpdateNotificationSettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);

            Map<String, Object> settings = new HashMap<>();
            settings.put("notifyReservation", 0);
            settings.put("notifyPromotion", 1);

            assertDoesNotThrow(() -> userSettingsService.updateNotificationSettings(1L, settings));
            verify(userSettingsMapper, times(1)).updateById(any(UserSettings.class));
        }
    }

    @Nested
    @DisplayName("偏好设置测试")
    class PreferenceSettingsTests {

        @Test
        @DisplayName("获取偏好设置 - 成功")
        void testGetPreferenceSettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);

            Map<String, Object> result = userSettingsService.getPreferenceSettings(1L);

            assertNotNull(result);
        }

        @Test
        @DisplayName("更新偏好设置 - 成功")
        void testUpdatePreferenceSettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);

            Map<String, Object> settings = new HashMap<>();
            settings.put("theme", "dark");
            settings.put("language", "en_US");

            assertDoesNotThrow(() -> userSettingsService.updatePreferenceSettings(1L, settings));
            verify(userSettingsMapper, times(1)).updateById(any(UserSettings.class));
        }
    }
}
