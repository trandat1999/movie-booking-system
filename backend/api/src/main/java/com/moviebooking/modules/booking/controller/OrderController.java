package com.moviebooking.modules.booking.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.booking.dto.request.ApplyPromotionRequest;
import com.moviebooking.modules.booking.dto.request.CreateOrderRequest;
import com.moviebooking.modules.booking.dto.response.MyTicketResponse;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.dto.response.OrderSummaryResponse;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Đặt vé")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Tạo đơn từ lượt giữ ghế (BKG05) — gọi lại với cùng lượt giữ trả về đơn đã tạo")
    @PostMapping("/orders")
    public ApiResponse<OrderResponse> create(
            @CurrentUser AuthenticatedUser user, @Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.ok(orderService.create(user.id(), request));
    }

    @Operation(summary = "Chi tiết đơn của tôi, kèm vé điện tử khi đã thanh toán (BKG08)")
    @GetMapping("/orders/{code}")
    public ApiResponse<OrderResponse> get(@CurrentUser AuthenticatedUser user, @PathVariable String code) {
        return ApiResponse.ok(orderService.get(user.id(), code));
    }

    @Operation(summary = "Áp mã giảm giá (BKG05) — giữ một lượt dùng; đơn hủy/hết hạn thì trả lượt")
    @PostMapping("/orders/{code}/promotion")
    public ApiResponse<OrderResponse> applyPromotion(
            @CurrentUser AuthenticatedUser user,
            @PathVariable String code,
            @Valid @RequestBody ApplyPromotionRequest request) {
        return ApiResponse.ok(orderService.applyPromotion(user.id(), code, request.code()));
    }

    @Operation(summary = "Bỏ mã giảm giá")
    @DeleteMapping("/orders/{code}/promotion")
    public ApiResponse<OrderResponse> removePromotion(@CurrentUser AuthenticatedUser user, @PathVariable String code) {
        return ApiResponse.ok(orderService.removePromotion(user.id(), code));
    }

    @Operation(summary = "Hủy đơn đang chờ thanh toán")
    @PostMapping("/orders/{code}/cancel")
    public ApiResponse<OrderResponse> cancel(@CurrentUser AuthenticatedUser user, @PathVariable String code) {
        return ApiResponse.ok(orderService.cancel(user.id(), code));
    }

    @Operation(summary = "Lịch sử đơn hàng (BKG09)")
    @GetMapping("/me/orders")
    public ApiResponse<PageResponse<OrderSummaryResponse>> myOrders(
            @CurrentUser AuthenticatedUser user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(orderService.listMine(user.id(), page, size));
    }

    @Operation(summary = "Vé của tôi (BKG10) — mặc định vé sắp chiếu, past=true cho vé đã xem")
    @GetMapping("/me/tickets")
    public ApiResponse<List<MyTicketResponse>> myTickets(
            @CurrentUser AuthenticatedUser user, @RequestParam(defaultValue = "false") boolean past) {
        return ApiResponse.ok(orderService.myTickets(user.id(), past));
    }
}
