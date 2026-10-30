package com.moviebooking.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Đăng nhập bằng email hoặc số điện thoại (USR02). */
public record LoginRequest(
        @NotBlank(message = "Vui lòng nhập email hoặc số điện thoại") @Size(max = 255) String identifier,
        @NotBlank(message = "Vui lòng nhập mật khẩu") @Size(max = 64) String password) {

    @Override
    public String toString() {
        return "LoginRequest[identifier=" + identifier + "]";
    }
}
