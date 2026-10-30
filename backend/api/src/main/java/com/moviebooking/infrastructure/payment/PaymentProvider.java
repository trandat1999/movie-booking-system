package com.moviebooking.infrastructure.payment;

import java.util.Map;

/**
 * Cổng thanh toán (PLAN.md 8.4). Tầng nghiệp vụ chọn provider theo {@link #code()}; provider không biết gì về
 * entity của module đặt vé (L5).
 */
public interface PaymentProvider {

    /** Mã provider, ví dụ {@code MOCK}, {@code VNPAY}. */
    String code();

    /** Tên hiển thị cho người dùng. */
    String displayName();

    /** false = chưa cấu hình (ví dụ thiếu mã merchant VNPay) — không cho chọn. */
    default boolean enabled() {
        return true;
    }

    /**
     * Tạo phiên thanh toán và trả về URL chuyển người dùng tới.
     *
     * @param clientIp IP người mua (VNPay yêu cầu)
     */
    PaymentSession createPayment(String orderCode, long amount, String description, String clientIp);

    /**
     * Kiểm tra chữ ký và đọc kết quả callback/IPN của cổng.
     *
     * @throws com.moviebooking.exception.BusinessException INVALID_PAYMENT_SIGNATURE nếu chữ ký sai
     */
    PaymentCallback verifyCallback(Map<String, String> params);

    /** true = hoàn tiền tự động qua API của cổng; false = admin hoàn thủ công trên trang merchant rồi xác nhận. */
    default boolean supportsAutoRefund() {
        return false;
    }

    /**
     * Hoàn tiền một giao dịch đã thành công.
     *
     * @return mã tham chiếu hoàn tiền phía cổng
     */
    default String refund(String transactionId, long amount, String reason) {
        throw new UnsupportedOperationException(code() + " does not support automatic refunds");
    }

    record PaymentSession(String redirectUrl) {}

    /**
     * @param transactionId mã giao dịch phía cổng — dùng để chống xử lý trùng (BR05)
     * @param success cổng báo thanh toán thành công
     */
    record PaymentCallback(
            String orderCode, String transactionId, long amount, boolean success, Map<String, String> raw) {}
}
