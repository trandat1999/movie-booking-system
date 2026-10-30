package com.moviebooking.modules.user.dto.request;

import com.moviebooking.modules.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull(message = "Vui lòng chọn trạng thái") UserStatus status) {}
