package com.moviebooking.modules.audit.dto.request;

/**
 * Một sự kiện cần ghi nhật ký. {@code actorId}, {@code actorEmail}, {@code ipAddress} có thể để trống — service
 * tự lấy từ người dùng và request hiện tại.
 */
public record AuditEntry(
        String action,
        String resource,
        String resourceId,
        Object data,
        Long actorId,
        String actorEmail,
        String ipAddress) {

    public static AuditEntry of(String action, String resource, String resourceId, Object data) {
        return new AuditEntry(action, resource, resourceId, data, null, null, null);
    }
}
