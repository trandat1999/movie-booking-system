package com.moviebooking.modules.setting.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * @param type {@code INTEGER} hoặc {@code OPTION}
 * @param options các giá trị cho phép khi type = OPTION
 */
public record SettingResponse(
        String key,
        String group,
        String description,
        String value,
        String type,
        Integer min,
        Integer max,
        List<String> options,
        Instant updatedAt) {}
