package com.moviebooking.modules.user.dto.response;

import com.moviebooking.modules.user.enums.UserStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AdminUserResponse(
        Long id,
        String email,
        String phone,
        String fullName,
        LocalDate dateOfBirth,
        UserStatus status,
        List<RoleSummaryResponse> roles,
        Instant lastLoginAt,
        Instant createdAt) {}
