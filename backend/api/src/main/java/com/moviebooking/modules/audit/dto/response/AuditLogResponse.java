package com.moviebooking.modules.audit.dto.response;

import java.time.Instant;
import java.util.Map;

public record AuditLogResponse(
        Long id,
        Long actorId,
        String actorEmail,
        String action,
        String resource,
        String resourceId,
        Map<String, Object> data,
        String ipAddress,
        Instant createdAt) {}
