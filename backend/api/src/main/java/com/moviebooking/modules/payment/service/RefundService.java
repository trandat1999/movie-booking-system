package com.moviebooking.modules.payment.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.payment.dto.response.RefundView;
import com.moviebooking.modules.payment.enums.RefundStatus;
import java.util.Optional;

/**
 * Hoàn tiền (BR10, BR17). Gọi từ module đặt vé khi hủy đơn đã thanh toán; không phụ thuộc ngược module đặt vé.
 * Cổng hỗ trợ hoàn tự động (giả lập) → COMPLETED ngay; cổng khác (VNPay) → PENDING chờ admin hoàn thủ công.
 */
public interface RefundService {

    /** Tạo yêu cầu hoàn cho đơn; gọi lại với cùng đơn trả về yêu cầu đã có (idempotent). */
    RefundView request(Long orderId, long amount, String reason, Long actorId);

    Optional<RefundView> findByOrder(Long orderId);

    /** Admin xác nhận đã hoàn thủ công. */
    RefundView markCompleted(Long refundId, Long actorId, String note);

    PageResponse<RefundView> list(RefundStatus status, int page, int size);
}
