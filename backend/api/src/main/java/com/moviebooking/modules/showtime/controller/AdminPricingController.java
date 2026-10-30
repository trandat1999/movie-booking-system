package com.moviebooking.modules.showtime.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.showtime.dto.request.HolidayRequest;
import com.moviebooking.modules.showtime.dto.request.UpdateBasePricesRequest;
import com.moviebooking.modules.showtime.dto.response.BasePriceResponse;
import com.moviebooking.modules.showtime.dto.response.HolidayResponse;
import com.moviebooking.modules.showtime.dto.response.PriceQuoteResponse;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.service.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin - Giá vé & ngày lễ")
@RestController
@RequestMapping("/api/v1/admin/pricing")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('showtime:write')")
public class AdminPricingController {

    private final PricingService pricingService;

    @Operation(summary = "Bảng giá cơ bản theo định dạng × loại ngày (SCHADM06)")
    @GetMapping("/base-prices")
    public ApiResponse<List<BasePriceResponse>> basePrices() {
        return ApiResponse.ok(pricingService.listBasePrices());
    }

    @Operation(summary = "Cập nhật bảng giá — áp dụng cho suất chiếu tạo sau đó")
    @PutMapping("/base-prices")
    @Audited(action = "BASE_PRICE_UPDATE", resource = "pricing", data = "#request")
    public ApiResponse<List<BasePriceResponse>> updateBasePrices(@Valid @RequestBody UpdateBasePricesRequest request) {
        return ApiResponse.ok(pricingService.updateBasePrices(request.items()));
    }

    @Operation(summary = "Xem trước giá và giờ kết thúc khi tạo suất chiếu")
    @GetMapping("/quote")
    public ApiResponse<PriceQuoteResponse> quote(
            @RequestParam Long roomId,
            @RequestParam(required = false) Long movieId,
            @RequestParam ScreenFormat format,
            @RequestParam Instant startAt) {
        return ApiResponse.ok(pricingService.preview(roomId, movieId, format, startAt));
    }

    @Operation(summary = "Danh sách ngày lễ trong năm")
    @GetMapping("/holidays")
    public ApiResponse<List<HolidayResponse>> holidays(@RequestParam int year) {
        return ApiResponse.ok(pricingService.listHolidays(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)));
    }

    @Operation(summary = "Thêm ngày lễ")
    @PostMapping("/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    @Audited(action = "HOLIDAY_CREATE", resource = "pricing", data = "#request")
    public ApiResponse<HolidayResponse> addHoliday(@Valid @RequestBody HolidayRequest request) {
        return ApiResponse.ok(pricingService.addHoliday(request));
    }

    @Operation(summary = "Xóa ngày lễ")
    @DeleteMapping("/holidays/{date}")
    @Audited(action = "HOLIDAY_DELETE", resource = "pricing", resourceId = "#date")
    public ApiResponse<Void> deleteHoliday(@PathVariable LocalDate date) {
        pricingService.deleteHoliday(date);
        return ApiResponse.ok();
    }
}
