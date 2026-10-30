package com.moviebooking.modules.notification.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.notification.dto.response.NotificationResponse;

/** Thông báo trong ứng dụng (NTF01) và email (NTF02). */
public interface NotificationService {

    /**
     * Tạo thông báo trong giao dịch riêng (được gọi từ listener sau commit) và đẩy realtime tới người dùng.
     */
    NotificationResponse notify(Long userId, String type, String title, String body, String link);

    PageResponse<NotificationResponse> list(Long userId, int page, int size);

    long unreadCount(Long userId);

    void markRead(Long userId, Long notificationId);

    void markAllRead(Long userId);

    /** Gửi email xác nhận đặt vé thành công; chạy bất đồng bộ, lỗi chỉ được ghi log. */
    void sendOrderPaidEmail(Long userId, String orderCode);
}
