package com.moviebooking.modules.booking.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.booking.dto.request.CreateOrderRequest;
import com.moviebooking.modules.booking.dto.response.MyTicketResponse;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.dto.response.OrderSummaryResponse;
import java.util.List;
import java.util.Map;

/** Đơn đặt vé và vé điện tử (BKG05–BKG10, BR04–BR09, BR17). */
public interface OrderService {

    /** Tạo đơn từ lượt giữ; gọi lại với cùng lượt giữ trả về đơn đã tạo (idempotent). */
    OrderResponse create(Long userId, CreateOrderRequest request);

    /** Đơn của chính người dùng; đơn của người khác coi như không tồn tại (chống IDOR). */
    OrderResponse get(Long userId, String code);

    /** Áp mã giảm giá vào đơn chờ thanh toán (BKG05); áp mã khác thì thay mã cũ. */
    OrderResponse applyPromotion(Long userId, String code, String promotionCode);

    OrderResponse removePromotion(Long userId, String code);

    /** Hủy đơn đang chờ thanh toán và trả ghế. */
    OrderResponse cancel(Long userId, String code);

    PageResponse<OrderSummaryResponse> listMine(Long userId, int page, int size);

    /** @param past true = vé các suất đã chiếu xong */
    List<MyTicketResponse> myTickets(Long userId, boolean past);

    /** Trưởng nhóm hủy đơn nhóm đã thanh toán theo chính sách (GRP21). */
    void cancelGroupOrder(Long orderId, Long actorId);

    /** Rạp hủy suất (SCHADM03): hủy mọi đơn, hoàn 100 %, trả ghế đang giữ. @return số đơn đã hủy */
    int cancelForShowtime(Long showtimeId, Long actorId);

    /** Admin xác nhận đã hoàn tiền thủ công → đơn REFUNDED (ORDADM-R). */
    com.moviebooking.modules.payment.dto.response.RefundView completeRefund(Long refundId, Long actorId, String note);

    /** Đơn đã thanh toán của suất đã chiếu xong → COMPLETED. @return số đơn */
    int completeFinishedOrders();

    /** Yêu cầu hoàn tiền cho trang quản trị; {@code status} null = tất cả. */
    com.moviebooking.common.response.PageResponse<com.moviebooking.modules.booking.dto.response.AdminRefundView> listRefunds(
            com.moviebooking.modules.payment.enums.RefundStatus status, int page, int size);

    // --- Quản trị (ORDADM) ---

    com.moviebooking.common.response.PageResponse<com.moviebooking.modules.booking.dto.response.AdminOrderRow> adminSearch(
            com.moviebooking.modules.booking.dto.request.AdminOrderQuery query);

    com.moviebooking.modules.booking.dto.response.AdminOrderDetail adminGet(String code);

    /** Admin hủy đơn: đơn đã thanh toán hoàn 100 % (ORDADM-R), đơn chờ thanh toán trả ghế. */
    com.moviebooking.modules.booking.dto.response.AdminOrderDetail adminCancel(String code, Long actorId);

    // --- Dùng cho module nhóm ---

    Long findIdByCode(String code);

    OrderSummaryResponse findSummary(Long orderId);

    /** Vé của đơn theo thứ tự ghế. */
    List<OrderTicket> ticketsOfOrder(Long orderId);

    /** Đổi người giữ vé (phân phối vé nhóm, GRP17) — vé xuất hiện trong "Vé của tôi" của người được giao. */
    void assignTicketHolders(Long orderId, Map<Long, TicketHolder> holders);

    record OrderTicket(Long id, String code, Long seatId, String seatLabel, Long holderUserId, String holderName) {}

    record TicketHolder(Long userId, String name) {}

    // --- Dùng cho module thanh toán ---

    /**
     * Đơn của người dùng còn thanh toán được (chờ thanh toán, chưa hết hạn).
     *
     * @throws com.moviebooking.exception.BusinessException ORDER_NOT_PAYABLE
     */
    PaymentTarget getPayable(Long userId, String code);

    /** Khóa dòng đơn (SELECT ... FOR UPDATE) trước khi ghi nhận giao dịch — hai callback không chạy song song. */
    PaymentTarget lockForPayment(String code);

    /**
     * Áp dụng thanh toán thành công cho đơn đã khóa: giữ được ghế → PAID và phát hành vé; ghế đã thuộc người
     * khác (thanh toán về sau khi hết hạn giữ) → REFUND_PENDING (BR17).
     */
    void applySuccessfulPayment(String code, String provider, String transactionId);

    /** Ghi lịch sử giao dịch thất bại / sai số tiền; đơn giữ nguyên trạng thái để người dùng thử lại. */
    void recordPaymentIssue(String code, String type, Map<String, Object> data);
}
