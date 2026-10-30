package com.moviebooking.modules.payment.service;

import com.moviebooking.modules.payment.dto.response.PaymentCallbackResponse;
import com.moviebooking.modules.payment.dto.response.PaymentProviderView;
import com.moviebooking.modules.payment.dto.response.PaymentSessionResponse;
import java.util.Map;

/** Thanh toán đơn qua cổng (BKG06, BKG07, BR04, BR05, BR17). */
public interface PaymentService {

    /** Tạo phiên thanh toán cho đơn đang chờ thanh toán của người dùng. */
    PaymentSessionResponse initiate(Long userId, String orderCode, String provider, String clientIp);

    /** Cổng thanh toán đang bật (đã cấu hình). */
    java.util.List<PaymentProviderView> providers();

    /**
     * Xử lý callback/IPN của cổng: kiểm tra chữ ký, ghi giao dịch (idempotent theo mã giao dịch), cập nhật đơn.
     */
    PaymentCallbackResponse handleCallback(String provider, Map<String, String> params);
}
