package com.moviebooking.modules.showtime.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.showtime.dto.response.PublicShowtimeDetailResponse;
import com.moviebooking.modules.showtime.dto.response.PublicShowtimeResponse;
import com.moviebooking.modules.showtime.dto.response.SeatMapResponse;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.service.PublicShowtimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** API công khai: lịch chiếu và sơ đồ ghế (không cần đăng nhập). */
@Tag(name = "Lịch chiếu")
@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {

    private final PublicShowtimeService publicShowtimeService;

    @Operation(summary = "Suất chiếu còn bán vé trong một ngày (giờ VN), lọc theo phim/rạp/định dạng (SCH03, SCH04)")
    @GetMapping
    public ApiResponse<List<PublicShowtimeResponse>> search(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) ScreenFormat format) {
        return ApiResponse.ok(publicShowtimeService.search(movieId, cinemaId, date, format));
    }

    @Operation(summary = "Thông tin suất chiếu và giá vé (SCH05, SCH07)")
    @GetMapping("/{id}")
    public ApiResponse<PublicShowtimeDetailResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(publicShowtimeService.get(id));
    }

    @Operation(summary = "Sơ đồ ghế và tình trạng ghế của suất chiếu (SCH06, BKG01)")
    @GetMapping("/{id}/seats")
    public ApiResponse<SeatMapResponse> seats(@PathVariable Long id) {
        return ApiResponse.ok(publicShowtimeService.getSeatMap(id));
    }
}
