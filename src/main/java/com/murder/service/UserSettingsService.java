package com.murder.service;

import java.util.Map;

public interface UserSettingsService {
    Map<String, Object> getPrivacySettings(Long userId);
    void updatePrivacySettings(Long userId, Map<String, Object> settings);
    Map<String, Object> getNotificationSettings(Long userId);
    void updateNotificationSettings(Long userId, Map<String, Object> settings);
    Map<String, Object> getPreferenceSettings(Long userId);
    void updatePreferenceSettings(Long userId, Map<String, Object> settings);
}
