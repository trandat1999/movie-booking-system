package com.moviebooking.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Định dạng phản hồi thống nhất của API.
 *
 * <p>Thành công: {@code {"success": true, "data": ...}}. Thất bại: {@code {"success": false, "error": {...}}}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, ApiError error) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> fail(ApiError error) {
        return new ApiResponse<>(false, null, error);
    }
}
