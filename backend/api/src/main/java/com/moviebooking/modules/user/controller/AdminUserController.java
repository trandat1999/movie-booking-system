package com.moviebooking.modules.user.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.user.dto.request.UpdateUserRolesRequest;
import com.moviebooking.modules.user.dto.request.UpdateUserStatusRequest;
import com.moviebooking.modules.user.dto.response.AdminUserResponse;
import com.moviebooking.modules.user.enums.UserStatus;
import com.moviebooking.modules.user.service.AdminUserService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Người dùng")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:read')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Danh sách người dùng, tìm theo email/tên/SĐT, lọc trạng thái và vai trò")
    @GetMapping
    public ApiResponse<PageResponse<AdminUserResponse>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(adminUserService.search(q, status, role, page, size));
    }

    @Operation(summary = "Chi tiết người dùng")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.get(id));
    }

    @Operation(summary = "Khóa / mở khóa tài khoản")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:write')")
    @Audited(action = "USER_STATUS_UPDATE", resource = "user", resourceId = "#id", data = "#request")
    public ApiResponse<AdminUserResponse> updateStatus(
            @CurrentUser AuthenticatedUser actor,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        return ApiResponse.ok(adminUserService.updateStatus(actor.id(), id, request.status()));
    }

    @Operation(summary = "Gán vai trò")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:write')")
    @Audited(action = "USER_ROLES_UPDATE", resource = "user", resourceId = "#id", data = "#request")
    public ApiResponse<AdminUserResponse> updateRoles(
            @CurrentUser AuthenticatedUser actor,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRolesRequest request) {
        return ApiResponse.ok(adminUserService.updateRoles(actor.id(), id, request.roleCodes()));
    }
}
