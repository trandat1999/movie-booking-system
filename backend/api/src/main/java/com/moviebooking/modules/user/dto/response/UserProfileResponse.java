package com.moviebooking.modules.user.dto.response;

import com.moviebooking.modules.user.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Hồ sơ của người dùng đang đăng nhập (kèm vai trò và quyền để frontend ẩn/hiện chức năng). */
public record UserProfileResponse(
        Long id,
        String email,
        String phone,
        String fullName,
        LocalDate dateOfBirth,
        String avatarUrl,
        UserStatus status,
        List<String> roles,
        List<String> permissions,
        Instant createdAt) {}
