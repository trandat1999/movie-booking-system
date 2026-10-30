package com.moviebooking.modules.notification.dto.response;

import java.time.Instant;

public record NotificationResponse(
        Long id, String type, String title, String body, String link, Instant readAt, Instant createdAt) {}
