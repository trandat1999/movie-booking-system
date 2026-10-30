package com.moviebooking.modules.booking.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.booking.entity.Order;
import com.moviebooking.modules.booking.entity.OrderEvent;
import com.moviebooking.modules.booking.entity.SeatHold;
import com.moviebooking.modules.booking.enums.HoldStatus;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.booking.event.HoldClosedEvent;
import com.moviebooking.modules.booking.event.OrderStatusChangedEvent;
import com.moviebooking.modules.booking.repository.OrderEventRepository;
import com.moviebooking.modules.booking.repository.OrderRepository;
import com.moviebooking.modules.booking.repository.SeatHoldRepository;
import com.moviebooking.modules.promotion.service.PromotionService;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Các bước chuyển trạng thái dùng chung giữa giữ ghế và đơn hàng; luôn chạy trong giao dịch của service gọi nó.
 * Thứ tự khóa: đơn hàng → lượt giữ → dòng ghế, để không deadlock với luồng thanh toán.
 */
@Component
@RequiredArgsConstructor
class BookingTransitions {

    private final OrderRepository orderRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final OrderEventRepository orderEventRepository;
    private final ShowtimeSeatService showtimeSeatService;
    private final PromotionService promotionService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /**
     * Đóng lượt giữ: đơn chờ thanh toán của nó chuyển sang {@code orderStatus}, ghế đang giữ trả về trống. Lượt giữ
     * chỉ được đọc sau khi đã khóa, nên nơi gọi không được nạp entity này trước (tránh đọc trạng thái cũ).
     *
     * @param ownerId nếu khác null: lượt giữ phải thuộc người này, nếu không coi như không tồn tại
     * @param onlyIfExpiredAt nếu khác null: chỉ đóng khi lượt giữ đã hết hạn tại thời điểm này
     * @return false nếu lượt giữ không còn ACTIVE (đã được xử lý trước đó) hoặc chưa hết hạn
     * @throws BusinessException HOLD_NOT_FOUND
     */
    boolean closeHold(
            Long holdId,
            Long ownerId,
            Instant onlyIfExpiredAt,
            HoldStatus holdStatus,
            OrderStatus orderStatus,
            String eventType,
            Long actorId) {
        Optional<Order> order = orderRepository.findForUpdateByHoldId(holdId);
        SeatHold hold = seatHoldRepository
                .findForUpdate(holdId)
                .filter(h -> ownerId == null || h.getUserId().equals(ownerId))
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLD_NOT_FOUND));
        if (hold.getStatus() != HoldStatus.ACTIVE
                || (onlyIfExpiredAt != null && !hold.getExpiresAt().isBefore(onlyIfExpiredAt))) {
            return false;
        }
        order.filter(o -> o.getStatus() == OrderStatus.PENDING_PAYMENT)
                .ifPresent(o -> changeStatus(o, orderStatus, eventType, actorId, null));
        hold.setStatus(holdStatus);
        showtimeSeatService.release(hold.getShowtimeId(), holdId);
        publishIfGroupHold(hold);
        return true;
    }

    /** Lượt giữ của nhóm đã đóng (không thành vé) → báo module nhóm trong cùng transaction. */
    void publishIfGroupHold(SeatHold hold) {
        if (hold.getGroupId() != null) {
            eventPublisher.publishEvent(new HoldClosedEvent(hold.getId(), hold.getGroupId(), hold.getStatus()));
        }
    }

    void changeStatus(Order order, OrderStatus to, String eventType, Long actorId, Map<String, Object> data) {
        OrderStatus from = order.getStatus();
        order.setStatus(to);
        if (to == OrderStatus.CANCELLED) {
            order.setCancelledAt(clock.instant());
        }
        recordEvent(order, eventType, from, to, actorId, data);
        // Đơn không còn được thanh toán / đã hủy → trả lượt dùng mã giảm giá
        if (to == OrderStatus.CANCELLED || to == OrderStatus.EXPIRED || to == OrderStatus.REFUND_PENDING) {
            promotionService.release(order.getId());
        }
        eventPublisher.publishEvent(
                new OrderStatusChangedEvent(
                        order.getId(), order.getCode(), order.getUserId(), order.getGroupId(), from, to, eventType));
    }

    void recordEvent(
            Order order, String type, OrderStatus from, OrderStatus to, Long actorId, Map<String, Object> data) {
        OrderEvent event = new OrderEvent();
        event.setOrderId(order.getId());
        event.setType(type);
        event.setFromStatus(from);
        event.setToStatus(to);
        event.setActorId(actorId);
        event.setData(data);
        event.setCreatedAt(clock.instant());
        orderEventRepository.save(event);
    }
}
