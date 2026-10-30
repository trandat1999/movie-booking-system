package com.moviebooking.modules.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateUserRolesRequest(
        @NotEmpty(message = "Người dùng phải có ít nhất một vai trò") Set<@NotBlank String> roleCodes) {}
