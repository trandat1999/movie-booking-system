package com.moviebooking.modules.cinema.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.cinema.dto.request.SeatTypeRequest;
import com.moviebooking.modules.cinema.dto.response.SeatTypeResponse;
import com.moviebooking.modules.cinema.service.SeatTypeService;
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

@Tag(name = "Admin - Loại ghế")
@RestController
@RequestMapping("/api/v1/admin/seat-types")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('cinema:write', 'showtime:write')")
public class AdminSeatTypeController {

    private final SeatTypeService seatTypeService;

    @Operation(summary = "Danh sách loại ghế kèm phụ thu (CIN05)")
    @GetMapping
    public ApiResponse<List<SeatTypeResponse>> list() {
        return ApiResponse.ok(seatTypeService.list());
    }

    @Operation(summary = "Sửa tên, phụ thu, màu loại ghế — áp dụng cho suất chiếu tạo sau đó")
    @PutMapping("/{code}")
    @PreAuthorize("hasAuthority('showtime:write')")
    @Audited(action = "SEAT_TYPE_UPDATE", resource = "seat_type", resourceId = "#code", data = "#request")
    public ApiResponse<SeatTypeResponse> update(@PathVariable String code, @Valid @RequestBody SeatTypeRequest request) {
        return ApiResponse.ok(seatTypeService.update(code, request));
    }
}
