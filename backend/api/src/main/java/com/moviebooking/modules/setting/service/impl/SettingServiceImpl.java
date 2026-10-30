package com.moviebooking.modules.setting.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.setting.dto.response.SettingResponse;
import com.moviebooking.modules.setting.entity.SystemSetting;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.repository.SystemSettingRepository;
import com.moviebooking.modules.setting.service.SettingService;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SystemSettingRepository settingRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> list() {
        Map<String, SystemSetting> stored = settingRepository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getKey, Function.identity()));
        return Arrays.stream(SettingKey.values())
                .filter(key -> stored.containsKey(key.getKey()))
                .map(key -> toResponse(key, stored.get(key.getKey())))
                .toList();
    }

    @Override
    @Transactional
    public SettingResponse update(String key, String value, Long actorId) {
        SettingKey settingKey = SettingKey.fromKey(key).orElseThrow(() -> new BusinessException(ErrorCode.SETTING_NOT_FOUND));
        String trimmed = value.trim();
        settingKey.validate(trimmed).ifPresent(message -> {
            throw new BusinessException(ErrorCode.INVALID_SETTING_VALUE, message);
        });
        SystemSetting setting = find(settingKey);
        setting.setValue(trimmed);
        setting.setUpdatedBy(actorId);
        setting.setUpdatedAt(clock.instant());
        return toResponse(settingKey, setting);
    }

    @Override
    @Transactional(readOnly = true)
    public int getInt(SettingKey key) {
        return Integer.parseInt(find(key).getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public String getString(SettingKey key) {
        return find(key).getValue();
    }

    private SystemSetting find(SettingKey key) {
        return settingRepository
                .findById(key.getKey())
                .orElseThrow(() -> new IllegalStateException("Missing setting " + key.getKey()));
    }

    private static SettingResponse toResponse(SettingKey key, SystemSetting setting) {
        return new SettingResponse(
                key.getKey(),
                key.getGroup(),
                key.getDescription(),
                setting.getValue(),
                key.isInteger() ? "INTEGER" : "OPTION",
                key.getMin(),
                key.getMax(),
                key.getOptions(),
                setting.getUpdatedAt());
    }
}
