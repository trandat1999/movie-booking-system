package com.moviebooking.modules.notification.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.notification.dto.response.NotificationResponse;
import com.moviebooking.modules.notification.service.NotificationService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Thông báo")
@RestController
@RequestMapping("/api/v1/me/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Thông báo của tôi, mới nhất trước (NTF01)")
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(notificationService.list(user.id(), page, size));
    }

    @Operation(summary = "Số thông báo chưa đọc")
    @GetMapping("/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount(@CurrentUser AuthenticatedUser user) {
        return ApiResponse.ok(Map.of("count", notificationService.unreadCount(user.id())));
    }

    @Operation(summary = "Đánh dấu đã đọc")
    @PostMapping("/{id}/read")
    public ApiResponse<Void> markRead(@CurrentUser AuthenticatedUser user, @PathVariable Long id) {
        notificationService.markRead(user.id(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc")
    @PostMapping("/read-all")
    public ApiResponse<Void> markAllRead(@CurrentUser AuthenticatedUser user) {
        notificationService.markAllRead(user.id());
        return ApiResponse.ok();
    }
}
