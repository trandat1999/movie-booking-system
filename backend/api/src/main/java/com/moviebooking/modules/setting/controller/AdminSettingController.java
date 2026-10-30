package com.moviebooking.modules.setting.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.setting.dto.request.UpdateSettingRequest;
import com.moviebooking.modules.setting.dto.response.SettingResponse;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Cấu hình hệ thống")
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('setting:write')")
public class AdminSettingController {

    private final SettingService settingService;

    @Operation(summary = "Danh sách tham số nghiệp vụ (SYS01)")
    @GetMapping
    public ApiResponse<List<SettingResponse>> list() {
        return ApiResponse.ok(settingService.list());
    }

    @Operation(summary = "Cập nhật một tham số")
    @PutMapping("/{key}")
    @Audited(action = "SETTING_UPDATE", resource = "setting", resourceId = "#key", data = "#request")
    public ApiResponse<SettingResponse> update(
            @CurrentUser AuthenticatedUser actor, @PathVariable String key, @Valid @RequestBody UpdateSettingRequest request) {
        return ApiResponse.ok(settingService.update(key, request.value(), actor.id()));
    }
}
