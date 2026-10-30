package com.moviebooking.infrastructure.payment;

import java.util.Map;

/**
 * Cổng thanh toán (PLAN.md 8.4). Tầng nghiệp vụ chọn provider theo {@link #code()}; provider không biết gì về
 * entity của module đặt vé (L5).
 */
public interface PaymentProvider {

    /** Mã provider, ví dụ {@code MOCK}, {@code VNPAY}. */
    String code();

    /** Tạo phiên thanh toán và trả về URL chuyển người dùng tới. */
    PaymentSession createPayment(String orderCode, long amount, String description);

    /**
     * Kiểm tra chữ ký và đọc kết quả callback/IPN của cổng.
     *
     * @throws com.moviebooking.exception.BusinessException INVALID_PAYMENT_SIGNATURE nếu chữ ký sai
     */
    PaymentCallback verifyCallback(Map<String, String> params);

    record PaymentSession(String redirectUrl) {}

    /**
     * @param transactionId mã giao dịch phía cổng — dùng để chống xử lý trùng (BR05)
     * @param success cổng báo thanh toán thành công
     */
    record PaymentCallback(
            String orderCode, String transactionId, long amount, boolean success, Map<String, String> raw) {}
}
