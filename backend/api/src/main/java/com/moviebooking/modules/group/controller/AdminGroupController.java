package com.moviebooking.modules.group.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.group.dto.response.AdminGroupDetail;
import com.moviebooking.modules.group.dto.response.AdminGroupRow;
import com.moviebooking.modules.group.enums.GroupStatus;
import com.moviebooking.modules.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — nhóm đặt vé")
@RestController
@RequestMapping("/api/v1/admin/groups")
@PreAuthorize("hasAuthority('group:read')")
@RequiredArgsConstructor
public class AdminGroupController {

    private final GroupService groupService;

    @Operation(summary = "Danh sách nhóm, lọc theo trạng thái / tên / mã mời (GRPADM)")
    @GetMapping
    public ApiResponse<PageResponse<AdminGroupRow>> search(
            @RequestParam(required = false) GroupStatus status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(groupService.adminSearch(status, q, page, size));
    }

    @Operation(summary = "Chi tiết nhóm, sơ đồ ghế, nhật ký thao tác, các lần đề xuất ghế (GRPADM)")
    @GetMapping("/{id}")
    public ApiResponse<AdminGroupDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(groupService.adminGet(id));
    }
}
