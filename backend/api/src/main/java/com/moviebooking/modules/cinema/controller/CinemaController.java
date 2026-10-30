package com.moviebooking.modules.cinema.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import com.moviebooking.modules.cinema.service.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** API công khai: danh sách và chi tiết rạp (không cần đăng nhập). */
@Tag(name = "Rạp")
@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    @Operation(summary = "Danh sách rạp đang hoạt động (SCH01)")
    @GetMapping
    public ApiResponse<List<CinemaResponse>> list() {
        return ApiResponse.ok(cinemaService.listPublic());
    }

    @Operation(summary = "Chi tiết rạp: địa chỉ, liên hệ, tiện ích (SCH02)")
    @GetMapping("/{id}")
    public ApiResponse<CinemaResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(cinemaService.getPublic(id));
    }
}
