package com.moviebooking.modules.showtime.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.showtime.dto.request.ShowtimeRequest;
import com.moviebooking.modules.showtime.dto.request.ShowtimeUpdateRequest;
import com.moviebooking.modules.showtime.dto.response.ShowtimeResponse;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import com.moviebooking.modules.showtime.service.ShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Lịch chiếu")
@RestController
@RequestMapping("/api/v1/admin/showtimes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('showtime:write')")
public class AdminShowtimeController {

    private final ShowtimeService showtimeService;

    @Operation(summary = "Danh sách suất chiếu, lọc theo rạp/phòng/phim/ngày (giờ VN) kèm thống kê ghế (SCHADM09)")
    @GetMapping
    public ApiResponse<PageResponse<ShowtimeResponse>> search(
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(showtimeService.search(cinemaId, roomId, movieId, date, status, page, size));
    }

    @Operation(summary = "Chi tiết suất chiếu")
    @GetMapping("/{id}")
    public ApiResponse<ShowtimeResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(showtimeService.get(id));
    }

    @Operation(summary = "Tạo suất chiếu (SCHADM01) — chặn trùng lịch kể cả thời gian dọn phòng")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(action = "SHOWTIME_CREATE", resource = "showtime", data = "#request")
    public ApiResponse<ShowtimeResponse> create(@Valid @RequestBody ShowtimeRequest request) {
        return ApiResponse.ok(showtimeService.create(request));
    }

    @Operation(summary = "Sửa giờ chiếu, định dạng, giá (SCHADM02)")
    @PutMapping("/{id}")
    @Audited(action = "SHOWTIME_UPDATE", resource = "showtime", resourceId = "#id", data = "#request")
    public ApiResponse<ShowtimeResponse> update(@PathVariable Long id, @Valid @RequestBody ShowtimeUpdateRequest request) {
        return ApiResponse.ok(showtimeService.update(id, request));
    }

    @Operation(summary = "Hủy suất chiếu (SCHADM03)")
    @PostMapping("/{id}/cancel")
    @Audited(action = "SHOWTIME_CANCEL", resource = "showtime", resourceId = "#id")
    public ApiResponse<ShowtimeResponse> cancel(@PathVariable Long id) {
        return ApiResponse.ok(showtimeService.cancel(id));
    }
}
