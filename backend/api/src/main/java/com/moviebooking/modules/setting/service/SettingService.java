package com.moviebooking.modules.setting.service;

import com.moviebooking.modules.setting.dto.response.SettingResponse;
import com.moviebooking.modules.setting.enums.SettingKey;
import java.util.List;

/** Tham số nghiệp vụ (SYS01) — nơi duy nhất cấu hình các tham số, module khác chỉ đọc. */
public interface SettingService {

    List<SettingResponse> list();

    SettingResponse update(String key, String value, Long actorId);

    int getInt(SettingKey key);

    String getString(SettingKey key);
}
