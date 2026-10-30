package com.moviebooking.modules.booking.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.audit.annotation.Audited;
import com.moviebooking.modules.booking.dto.request.CompleteRefundRequest;
import com.moviebooking.modules.booking.dto.response.AdminRefundView;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.payment.dto.response.RefundView;
import com.moviebooking.modules.payment.enums.RefundStatus;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Quản trị — hoàn tiền")
@RestController
@RequestMapping("/api/v1/admin/refunds")
@PreAuthorize("hasAuthority('order:refund')")
@RequiredArgsConstructor
public class AdminRefundController {

    private final OrderService orderService;

    @Operation(summary = "Yêu cầu hoàn tiền (ORDADM-R) — PENDING là đơn cần hoàn thủ công trên trang merchant")
    @GetMapping
    public ApiResponse<PageResponse<AdminRefundView>> list(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(orderService.listRefunds(status, page, size));
    }

    @Operation(summary = "Xác nhận đã hoàn tiền thủ công → đơn REFUNDED")
    @PostMapping("/{id}/complete")
    @Audited(action = "REFUND_COMPLETE", resource = "refund", resourceId = "#id", data = "#request")
    public ApiResponse<RefundView> complete(
            @CurrentUser AuthenticatedUser user,
            @PathVariable Long id,
            @Valid @RequestBody CompleteRefundRequest request) {
        return ApiResponse.ok(orderService.completeRefund(id, user.id(), request.note()));
    }
}
