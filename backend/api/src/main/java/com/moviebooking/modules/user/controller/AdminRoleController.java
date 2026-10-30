package com.moviebooking.modules.user.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.user.dto.response.RoleResponse;
import com.moviebooking.modules.user.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Vai trò")
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('role:read')")
public class AdminRoleController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Danh sách vai trò kèm quyền")
    @GetMapping
    public ApiResponse<List<RoleResponse>> list() {
        return ApiResponse.ok(adminUserService.listRoles());
    }
}
