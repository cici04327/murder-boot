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
            assertEquals(1, result.get("showProfile"));
            assertEquals(1, result.get("showHistory"));
            assertEquals(1, result.get("showFavorites"));
        }

        @Test
        @DisplayName("获取隐私设置 - 不存在则返回默认值")
        void testGetPrivacySettings_DefaultValues() {
            when(userSettingsMapper.selectOne(any())).thenReturn(null);
            when(userSettingsMapper.insert(any(UserSettings.class))).thenReturn(1);

            Map<String, Object> result = userSettingsService.getPrivacySettings(1L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(userSettingsMapper, times(1)).insert(any(UserSettings.class));
        }

        @Test
        @DisplayName("更新隐私设置 - 成功")
        void testUpdatePrivacySettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);
            ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);

            Map<String, Object> settings = new HashMap<>();
            settings.put("showProfile", 0);
            settings.put("showHistory", 0);

            userSettingsService.updatePrivacySettings(1L, settings);
            verify(userSettingsMapper, times(1)).updateById(settingsCaptor.capture());

            UserSettings updated = settingsCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertEquals(1L, updated.getUserId());
            assertTrue(updated.getPrivacySettings().contains("\"showProfile\":0"));
            assertTrue(updated.getPrivacySettings().contains("\"showHistory\":0"));
        }

        @Test
        @DisplayName("更新隐私设置 - 不存在则创建")
        void testUpdatePrivacySettings_CreateNew() {
            when(userSettingsMapper.selectOne(any())).thenReturn(null);
            when(userSettingsMapper.insert(any(UserSettings.class))).thenReturn(1);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);
            ArgumentCaptor<UserSettings> insertCaptor = ArgumentCaptor.forClass(UserSettings.class);
            ArgumentCaptor<UserSettings> updateCaptor = ArgumentCaptor.forClass(UserSettings.class);

            Map<String, Object> settings = new HashMap<>();
            settings.put("showProfile", 1);

            userSettingsService.updatePrivacySettings(1L, settings);
            verify(userSettingsMapper, times(1)).insert(insertCaptor.capture());
            verify(userSettingsMapper, times(1)).updateById(updateCaptor.capture());

            assertEquals(1L, insertCaptor.getValue().getUserId());
            assertEquals(1L, updateCaptor.getValue().getUserId());
            assertTrue(updateCaptor.getValue().getPrivacySettings().contains("\"showProfile\":1"));
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
            assertEquals(1, result.get("notifyReservation"));
            assertEquals(1, result.get("notifyPromotion"));
            assertEquals(1, result.get("notifySystem"));
        }

        @Test
        @DisplayName("更新通知设置 - 成功")
        void testUpdateNotificationSettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);
            ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);

            Map<String, Object> settings = new HashMap<>();
            settings.put("notifyReservation", 0);
            settings.put("notifyPromotion", 1);

            userSettingsService.updateNotificationSettings(1L, settings);
            verify(userSettingsMapper, times(1)).updateById(settingsCaptor.capture());

            UserSettings updated = settingsCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertTrue(updated.getNotificationSettings().contains("\"notifyReservation\":0"));
            assertTrue(updated.getNotificationSettings().contains("\"notifyPromotion\":1"));
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
            assertEquals("light", result.get("theme"));
            assertEquals("zh_CN", result.get("language"));
        }

        @Test
        @DisplayName("更新偏好设置 - 成功")
        void testUpdatePreferenceSettings_Success() {
            when(userSettingsMapper.selectOne(any())).thenReturn(testSettings);
            when(userSettingsMapper.updateById(any(UserSettings.class))).thenReturn(1);
            ArgumentCaptor<UserSettings> settingsCaptor = ArgumentCaptor.forClass(UserSettings.class);

            Map<String, Object> settings = new HashMap<>();
            settings.put("theme", "dark");
            settings.put("language", "en_US");

            userSettingsService.updatePreferenceSettings(1L, settings);
            verify(userSettingsMapper, times(1)).updateById(settingsCaptor.capture());

            UserSettings updated = settingsCaptor.getValue();
            assertEquals(1L, updated.getId());
            assertTrue(updated.getPreferenceSettings().contains("\"theme\":\"dark\""));
            assertTrue(updated.getPreferenceSettings().contains("\"language\":\"en_US\""));
        }
    }
}
