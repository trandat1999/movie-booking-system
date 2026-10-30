package com.moviebooking.modules.booking.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.booking.dto.request.AdminOrderQuery;
import com.moviebooking.modules.booking.dto.response.AdminOrderDetail;
import com.moviebooking.modules.booking.dto.response.AdminOrderRow;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — đơn đặt vé")
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAuthority('order:read')")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @Operation(summary = "Tìm kiếm, lọc đơn (ORDADM)")
    @GetMapping
    public ApiResponse<PageResponse<AdminOrderRow>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.adminSearch(
                new AdminOrderQuery(q, email, status, from, to, cinemaId, movieId, page, size)));
    }

    @Operation(summary = "Chi tiết đơn, lịch sử thay đổi, giao dịch (ORDADM)")
    @GetMapping("/{code}")
    public ApiResponse<AdminOrderDetail> get(@PathVariable String code) {
        return ApiResponse.ok(orderService.adminGet(code));
    }

    @Operation(summary = "Hủy đơn và hoàn tiền 100 % (ORDADM-R)")
    @PostMapping("/{code}/cancel")
    @PreAuthorize("hasAuthority('order:refund')")
    @Audited(action = "ORDER_ADMIN_CANCEL", resource = "order", resourceId = "#code")
    public ApiResponse<AdminOrderDetail> cancel(@CurrentUser AuthenticatedUser user, @PathVariable String code) {
        return ApiResponse.ok(orderService.adminCancel(code, user.id()));
    }
}
