package com.moviebooking.modules.payment.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.modules.payment.dto.request.PayOrderRequest;
import com.moviebooking.modules.payment.dto.response.PaymentCallbackResponse;
import com.moviebooking.modules.payment.dto.response.PaymentSessionResponse;
import com.moviebooking.modules.payment.service.PaymentService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Thanh toán")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Bắt đầu thanh toán đơn (BKG06) — trả về URL của cổng thanh toán")
    @PostMapping("/orders/{code}/pay")
    public ApiResponse<PaymentSessionResponse> pay(
            @CurrentUser AuthenticatedUser user,
            @PathVariable String code,
            @Valid @RequestBody PayOrderRequest request) {
        return ApiResponse.ok(paymentService.initiate(user.id(), code, request.provider()));
    }

    @Operation(summary = "IPN/callback của cổng thanh toán (BKG07) — công khai, xác thực bằng chữ ký; idempotent")
    @PostMapping("/payments/{provider}/ipn")
    public ApiResponse<PaymentCallbackResponse> ipn(
            @PathVariable String provider, @RequestBody Map<String, String> params) {
        return ApiResponse.ok(paymentService.handleCallback(provider, params));
    }
}
