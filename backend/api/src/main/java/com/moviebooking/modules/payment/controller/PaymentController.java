package com.moviebooking.modules.payment.controller;

import com.moviebooking.common.response.ApiResponse;
import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.modules.payment.dto.request.PayOrderRequest;
import com.moviebooking.modules.payment.dto.response.PaymentCallbackResponse;
import com.moviebooking.modules.payment.dto.response.PaymentProviderView;
import com.moviebooking.modules.payment.dto.response.PaymentSessionResponse;
import com.moviebooking.modules.payment.service.PaymentService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Thanh toán")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "Các cổng thanh toán đang bật")
    @GetMapping("/payments/providers")
    public ApiResponse<List<PaymentProviderView>> providers() {
        return ApiResponse.ok(paymentService.providers());
    }

    @Operation(summary = "Bắt đầu thanh toán đơn (BKG06) — trả về URL của cổng thanh toán")
    @PostMapping("/orders/{code}/pay")
    public ApiResponse<PaymentSessionResponse> pay(
            @CurrentUser AuthenticatedUser user,
            @PathVariable String code,
            @Valid @RequestBody PayOrderRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(paymentService.initiate(
                user.id(), code, request.provider(), ClientInfo.from(httpRequest).ipAddress()));
    }

    @Operation(summary = "Kết quả thanh toán gửi từ trình duyệt (cổng giả lập, trang return của VNPay) — xác thực bằng chữ ký; idempotent")
    @PostMapping("/payments/{provider}/ipn")
    public ApiResponse<PaymentCallbackResponse> ipn(
            @PathVariable String provider, @RequestBody Map<String, String> params) {
        return ApiResponse.ok(paymentService.handleCallback(provider, params));
    }

    /**
     * IPN server–server của VNPay (GET, tham số trên query). VNPay chờ phản hồi dạng {@code {RspCode, Message}}:
     * 00 = đã ghi nhận (kể cả trùng), 97 = sai chữ ký, 01 = không có đơn, 99 = lỗi khác.
     */
    @Operation(summary = "IPN của VNPay")
    @GetMapping("/payments/VNPAY/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        try {
            paymentService.handleCallback("VNPAY", params);
            return Map.of("RspCode", "00", "Message", "Confirm Success");
        } catch (BusinessException ex) {
            String code = switch (ex.getErrorCode()) {
                case INVALID_PAYMENT_SIGNATURE -> "97";
                case ORDER_NOT_FOUND -> "01";
                default -> "99";
            };
            return Map.of("RspCode", code, "Message", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error("VNPay IPN failed", ex);
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }
}
