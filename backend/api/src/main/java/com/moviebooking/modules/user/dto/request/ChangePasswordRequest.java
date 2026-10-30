package com.moviebooking.modules.user.dto.request;

import com.moviebooking.common.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(
        @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại") String currentPassword,
        @NotBlank(message = "Vui lòng nhập mật khẩu mới")
                @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
                String newPassword) {}
