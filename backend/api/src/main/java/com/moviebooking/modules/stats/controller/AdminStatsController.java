package com.moviebooking.modules.stats.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.stats.dto.response.GroupStats;
import com.moviebooking.modules.stats.dto.response.OverviewStats;
import com.moviebooking.modules.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — thống kê")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAuthority('stats:read')")
@RequiredArgsConstructor
public class AdminStatsController {

    private final StatsService statsService;

    @Operation(summary = "Dashboard tổng quan (ADM01): doanh thu, vé, lấp đầy, thanh toán thất bại, đơn trung bình")
    @GetMapping("/stats/overview")
    public ApiResponse<OverviewStats> overview(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long movieId) {
        return ApiResponse.ok(statsService.overview(from, to, cinemaId, movieId));
    }

    @Operation(summary = "Thống kê nhóm (ADM02): chuyển đổi, quy mô, tỷ lệ cụm liền kề, thời gian tạo → đặt, lý do thất bại")
    @GetMapping("/stats/groups")
    public ApiResponse<GroupStats> groups(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long movieId) {
        return ApiResponse.ok(statsService.groups(from, to, movieId));
    }

    @Operation(summary = "Xuất đơn hàng ra CSV mở được bằng Excel (ADM03)")
    @GetMapping("/reports/orders.csv")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long movieId) {
        byte[] body = statsService.exportOrdersCsv(from, to, cinemaId, movieId);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("don-hang.csv").build().toString())
                .body(body);
    }
}
