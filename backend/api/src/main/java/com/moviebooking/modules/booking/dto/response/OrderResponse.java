package com.moviebooking.modules.booking.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.payment.enums.RefundStatus;
import java.time.Instant;
import java.util.List;

/**
 * @param groupId khác null nếu là đơn của nhóm (chỉ hủy được từ trang nhóm)
 * @param cancellableUntil hạn tự hủy vé (giờ chiếu − {@code cancel_before_hours}); null nếu đơn không tự hủy được
 * @param qrImage QR của cả đơn (soát vé cả nhóm một lần, CHK01) — khi đơn đã thanh toán
 * @param refund yêu cầu hoàn tiền (nếu có)
 */
public record OrderResponse(
        String code,
        OrderStatus status,
        BookingShowtimeInfo showtime,
        List<SeatLine> items,
        long subtotal,
        long discount,
        String promotionCode,
        long total,
        Instant expiresAt,
        Instant paidAt,
        Instant cancelledAt,
        Instant createdAt,
        List<TicketResponse> tickets,
        Long groupId,
        Instant cancellableUntil,
        String qrImage,
        RefundInfo refund,
        Instant serverTime) {

    public record RefundInfo(long amount, RefundStatus status, Instant processedAt) {}
}
