package com.murder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.murder.entity.UserSettings;
import com.murder.mapper.UserSettingsMapper;
import com.murder.service.UserSettingsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserSettingsServiceImpl implements UserSettingsService {

    @Autowired
    private UserSettingsMapper userSettingsMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private UserSettings getOrCreate(Long userId) {
        LambdaQueryWrapper<UserSettings> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserSettings::getUserId, userId);
        UserSettings settings = userSettingsMapper.selectOne(wrapper);
        if (settings == null) {
            settings = UserSettings.builder().userId(userId).build();
            userSettingsMapper.insert(settings);
        }
        return settings;
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.error("解析设置JSON失败", e);
            return new HashMap<>();
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            log.error("序列化设置JSON失败", e);
            return "{}";
        }
    }

    @Override
    public Map<String, Object> getPrivacySettings(Long userId) {
        return parseJson(getOrCreate(userId).getPrivacySettings());
    }

    @Override
    public void updatePrivacySettings(Long userId, Map<String, Object> settings) {
        UserSettings us = getOrCreate(userId);
        us.setPrivacySettings(toJson(settings));
        userSettingsMapper.updateById(us);
    }

    @Override
    public Map<String, Object> getNotificationSettings(Long userId) {
        return parseJson(getOrCreate(userId).getNotificationSettings());
    }

    @Override
    public void updateNotificationSettings(Long userId, Map<String, Object> settings) {
        UserSettings us = getOrCreate(userId);
        us.setNotificationSettings(toJson(settings));
        userSettingsMapper.updateById(us);
    }

    @Override
    public Map<String, Object> getPreferenceSettings(Long userId) {
        return parseJson(getOrCreate(userId).getPreferenceSettings());
    }

    @Override
    public void updatePreferenceSettings(Long userId, Map<String, Object> settings) {
        UserSettings us = getOrCreate(userId);
        us.setPreferenceSettings(toJson(settings));
        userSettingsMapper.updateById(us);
    }
}
