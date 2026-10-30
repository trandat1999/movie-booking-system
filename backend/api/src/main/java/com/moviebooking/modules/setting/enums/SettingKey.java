package com.moviebooking.modules.setting.enums;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.Getter;

/**
 * Tham số nghiệp vụ cấu hình được (PLAN.md mục 9). Kiểu, giới hạn và mô tả khai báo tại đây; giá trị nằm ở bảng
 * {@code system_settings}.
 */
@Getter
public enum SettingKey {
    HOLD_TTL_SINGLE_SEC("hold_ttl_single_sec", "Giữ ghế", "Thời gian giữ ghế khi đặt vé cá nhân (giây)", 60, 1800),
    HOLD_TTL_GROUP_SEC("hold_ttl_group_sec", "Giữ ghế", "Thời gian giữ ghế khi đặt vé nhóm (giây)", 60, 3600),
    MAX_SEATS_PER_BOOKING("max_seats_per_booking", "Giữ ghế", "Số ghế tối đa trong một đơn cá nhân", 1, 20),
    MAX_ACTIVE_HOLDS_PER_USER("max_active_holds_per_user", "Giữ ghế", "Số lượt giữ ghế đang hiệu lực tối đa / người", 1, 10),
    ORPHAN_SEAT_RULE(
            "orphan_seat_rule",
            "Giữ ghế",
            "Quy tắc ghế lẻ khi đặt vé cá nhân: block = chặn, warn = cảnh báo, off = tắt",
            List.of("block", "warn", "off")),
    GROUP_MAX_SIZE("group_max_size", "Đặt vé nhóm", "Số vé tối đa của một nhóm", 2, 50),
    MAX_ACTIVE_GROUPS_PER_USER("max_active_groups_per_user", "Đặt vé nhóm", "Số nhóm đang hoạt động tối đa / người", 1, 20),
    GROUP_SPLIT_MAX_ROWS("group_split_max_rows", "Đặt vé nhóm", "Số hàng tối đa khi chia nhóm ngồi nhiều hàng", 1, 5),
    RSVP_REMINDER_BEFORE_MIN("rsvp_reminder_before_min", "Đặt vé nhóm", "Nhắc thành viên trước hạn chốt (phút)", 0, 1440),
    CLEANING_BUFFER_MIN("cleaning_buffer_min", "Lịch chiếu", "Thời gian dọn phòng giữa hai suất (phút)", 0, 120),
    SALE_CLOSE_AFTER_START_MIN(
            "sale_close_after_start_min", "Lịch chiếu", "Ngừng bán vé sau khi suất bắt đầu (phút)", 0, 60),
    CANCEL_BEFORE_HOURS("cancel_before_hours", "Hủy & hoàn tiền", "Được hủy vé nếu trước giờ chiếu ít nhất (giờ)", 0, 168),
    REFUND_PERCENT("refund_percent", "Hủy & hoàn tiền", "Tỷ lệ hoàn tiền khi hủy hợp lệ (%)", 0, 100);

    private final String key;
    private final String group;
    private final String description;
    private final Integer min;
    private final Integer max;
    private final List<String> options;

    SettingKey(String key, String group, String description, int min, int max) {
        this(key, group, description, min, max, null);
    }

    SettingKey(String key, String group, String description, List<String> options) {
        this(key, group, description, null, null, options);
    }

    SettingKey(String key, String group, String description, Integer min, Integer max, List<String> options) {
        this.key = key;
        this.group = group;
        this.description = description;
        this.min = min;
        this.max = max;
        this.options = options;
    }

    public boolean isInteger() {
        return options == null;
    }

    /** @return thông báo lỗi nếu giá trị không hợp lệ */
    public Optional<String> validate(String value) {
        if (value == null || value.isBlank()) {
            return Optional.of("Giá trị không được để trống");
        }
        if (!isInteger()) {
            return options.contains(value) ? Optional.empty() : Optional.of("Chỉ chấp nhận: " + String.join(", ", options));
        }
        try {
            int number = Integer.parseInt(value.trim());
            return number < min || number > max
                    ? Optional.of("Giá trị phải trong khoảng %d–%d".formatted(min, max))
                    : Optional.empty();
        } catch (NumberFormatException ex) {
            return Optional.of("Giá trị phải là số nguyên");
        }
    }

    public static Optional<SettingKey> fromKey(String key) {
        return Arrays.stream(values()).filter(k -> k.key.equals(key)).findFirst();
    }
}
