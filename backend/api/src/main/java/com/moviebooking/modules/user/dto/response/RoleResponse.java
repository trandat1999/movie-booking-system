package com.moviebooking.modules.user.dto.response;

import java.util.List;

public record RoleResponse(Long id, String code, String name, String description, List<PermissionResponse> permissions) {}
