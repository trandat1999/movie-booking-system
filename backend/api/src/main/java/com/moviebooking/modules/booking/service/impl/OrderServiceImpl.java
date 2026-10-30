package com.moviebooking.modules.booking.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.booking.dto.request.CreateOrderRequest;
import com.moviebooking.modules.booking.dto.response.AdminRefundView;
import com.moviebooking.modules.booking.dto.response.MyTicketResponse;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.dto.response.OrderSummaryResponse;
import com.moviebooking.modules.booking.entity.Order;
import com.moviebooking.modules.booking.entity.OrderItem;
import com.moviebooking.modules.booking.entity.SeatHold;
import com.moviebooking.modules.booking.entity.Ticket;
import com.moviebooking.modules.booking.enums.HoldStatus;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.booking.repository.OrderRepository;
import com.moviebooking.modules.booking.repository.SeatHoldRepository;
import com.moviebooking.modules.booking.repository.TicketRepository;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.booking.service.PaymentTarget;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import com.moviebooking.modules.booking.enums.TicketStatus;
import com.moviebooking.modules.payment.dto.response.RefundView;
import com.moviebooking.modules.payment.enums.RefundStatus;
import com.moviebooking.modules.payment.service.RefundService;
import com.moviebooking.modules.promotion.service.PromotionService;
import com.moviebooking.modules.payment.service.PaymentQueryService;
import com.moviebooking.modules.user.dto.response.UserBrief;
import com.moviebooking.modules.user.service.UserService;
import com.moviebooking.modules.booking.repository.OrderEventRepository;
import com.moviebooking.modules.booking.dto.request.AdminOrderQuery;
import com.moviebooking.modules.booking.dto.response.AdminOrderDetail;
import com.moviebooking.modules.booking.dto.response.AdminOrderRow;
import com.moviebooking.common.util.TimeZones;
import org.springframework.data.jpa.domain.Specification;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final int MAX_PAGE_SIZE = 50;

    private final OrderRepository orderRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final TicketRepository ticketRepository;
    private final ShowtimeSeatService showtimeSeatService;
    private final RoomService roomService;
    private final BookingTransitions transitions;
    private final BookingViews views;
    private final RefundService refundService;
    private final PromotionService promotionService;
    private final PaymentQueryService paymentQueryService;
    private final UserService userService;
    private final OrderEventRepository orderEventRepository;
    private final SettingService settingService;
    private final Clock clock;

    @Override
    @Transactional
    public OrderResponse create(Long userId, CreateOrderRequest request) {
        Long holdId = request.holdId();
        Optional<OrderResponse> existing = existingOrderOfHold(userId, holdId);
        if (existing.isPresent()) {
            return existing.get();
        }
        SeatHold hold = seatHoldRepository
                .findForUpdate(holdId)
                .filter(h -> h.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.HOLD_NOT_FOUND));
        // Kiểm tra lại sau khi khóa: request song song cùng lượt giữ có thể vừa tạo đơn
        existing = existingOrderOfHold(userId, holdId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Instant now = clock.instant();
        List<Long> seatIds = showtimeSeatService.seatIdsOfHold(holdId);
        if (!hold.isActiveAt(now) || seatIds.isEmpty()) {
            throw new BusinessException(ErrorCode.HOLD_EXPIRED);
        }

        Showtime showtime = showtimeSeatService.getBookable(hold.getShowtimeId());
        Map<Long, Seat> roomSeats = roomService.listSeats(showtime.getRoom().getId()).stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));
        Order order = new Order();
        order.setCode(BookingCodes.orderCode());
        order.setUserId(userId);
        order.setShowtime(showtime);
        order.setHoldId(holdId);
        order.setGroupId(hold.getGroupId());
        order.setExpiresAt(hold.getExpiresAt());
        seatIds.stream()
                .map(roomSeats::get)
                .sorted(Comparator.comparingInt(Seat::getGridRow).thenComparingInt(Seat::getGridCol))
                .forEach(seat -> {
                    Long price = showtime.getPrices().get(seat.getSeatTypeCode());
                    if (price == null) {
                        throw new BusinessException(ErrorCode.SHOWTIME_NOT_BOOKABLE, "Suất chiếu chưa có giá cho loại ghế "
                                + seat.getSeatTypeCode());
                    }
                    OrderItem item = new OrderItem();
                    item.setSeatId(seat.getId());
                    item.setSeatLabel(seat.label());
                    item.setSeatTypeCode(seat.getSeatTypeCode());
                    item.setPrice(price);
                    order.addItem(item);
                });
        long subtotal = order.getItems().stream().mapToLong(OrderItem::getPrice).sum();
        order.setSubtotal(subtotal);
        order.setDiscount(0);
        order.setTotal(subtotal);
        orderRepository.saveAndFlush(order);
        transitions.recordEvent(
                order, "CREATED", null, OrderStatus.PENDING_PAYMENT, userId, Map.of("holdId", holdId));
        return views.order(order, now);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse get(Long userId, String code) {
        return views.order(findOwned(userId, code), clock.instant());
    }

    @Override
    @Transactional
    public OrderResponse cancel(Long userId, String code) {
        Order order = orderRepository
                .findForUpdateByCode(code)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.PAID) {
            // Đơn nhóm chỉ trưởng nhóm hủy từ trang nhóm để nhóm cũng chuyển trạng thái (GRP21)
            if (order.getGroupId() != null) {
                throw new BusinessException(ErrorCode.GROUP_ORDER_CANCEL_VIA_GROUP);
            }
            cancelPaidOrder(order, userId, "CANCELLED_BY_USER", true);
            return views.order(findOwned(userId, code), clock.instant());
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        transitions.changeStatus(order, OrderStatus.CANCELLED, "CANCELLED_BY_USER", userId, null);
        transitions.closeHold(
                order.getHoldId(),
                userId,
                null,
                HoldStatus.RELEASED,
                OrderStatus.CANCELLED,
                "CANCELLED_BY_USER",
                userId);
        return views.order(findOwned(userId, code), clock.instant());
    }

    @Override
    @Transactional
    public OrderResponse applyPromotion(Long userId, String code, String promotionCode) {
        Order order = payableOwned(userId, code);
        PromotionService.Applied applied = promotionService.reserve(
                userId, order.getId(), promotionCode, order.getSubtotal(), order.getItems().size());
        order.setPromotionCode(applied.code());
        order.setDiscount(applied.discount());
        order.setTotal(order.getSubtotal() - applied.discount());
        transitions.recordEvent(order, "PROMOTION_APPLIED", order.getStatus(), order.getStatus(), userId,
                Map.of("code", applied.code(), "discount", applied.discount()));
        return views.order(findOwned(userId, code), clock.instant());
    }

    @Override
    @Transactional
    public OrderResponse removePromotion(Long userId, String code) {
        Order order = payableOwned(userId, code);
        if (order.getPromotionCode() != null) {
            promotionService.release(order.getId());
            transitions.recordEvent(order, "PROMOTION_REMOVED", order.getStatus(), order.getStatus(), userId,
                    Map.of("code", order.getPromotionCode()));
            order.setPromotionCode(null);
            order.setDiscount(0);
            order.setTotal(order.getSubtotal());
        }
        return views.order(findOwned(userId, code), clock.instant());
    }

    /** Đơn của người dùng, đang chờ thanh toán và chưa hết hạn — đã khóa dòng. */
    private Order payableOwned(Long userId, String code) {
        Order order = orderRepository
                .findForUpdateByCode(code)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || !order.getExpiresAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE);
        }
        return order;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminOrderRow> adminSearch(AdminOrderQuery query) {
        Specification<Order> spec = (root, cq, cb) -> cb.conjunction();
        if (query.q() != null && !query.q().isBlank()) {
            String like = "%" + query.q().trim().toUpperCase(java.util.Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) -> cb.like(root.get("code"), like));
        }
        if (query.email() != null && !query.email().isBlank()) {
            Long userId = userService.findIdByEmail(query.email()).orElse(-1L);
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (query.status() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), query.status()));
        }
        if (query.from() != null) {
            Instant from = query.from().atStartOfDay(TimeZones.VIETNAM).toInstant();
            spec = spec.and((root, cq, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (query.to() != null) {
            Instant to = query.to().plusDays(1).atStartOfDay(TimeZones.VIETNAM).toInstant();
            spec = spec.and((root, cq, cb) -> cb.lessThan(root.get("createdAt"), to));
        }
        if (query.movieId() != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("showtime").get("movie").get("id"), query.movieId()));
        }
        if (query.cinemaId() != null) {
            spec = spec.and((root, cq, cb) ->
                    cb.equal(root.get("showtime").get("room").get("cinema").get("id"), query.cinemaId()));
        }
        PageRequest pageable = PageRequest.of(
                Math.max(query.page(), 0),
                Math.min(Math.max(query.size(), 1), MAX_PAGE_SIZE),
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<Order> page = orderRepository.findAll(spec, pageable);
        Map<Long, UserBrief> customers =
                userService.briefs(page.getContent().stream().map(Order::getUserId).collect(Collectors.toSet()));
        return PageResponse.from(page.map(o -> new AdminOrderRow(
                o.getCode(),
                o.getStatus(),
                views.showtime(o.getShowtime()),
                o.getItems().size(),
                o.getTotal(),
                o.getDiscount(),
                o.getPromotionCode(),
                o.getGroupId(),
                customers.get(o.getUserId()),
                o.getCreatedAt(),
                o.getPaidAt())));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetail adminGet(String code) {
        Order order = orderRepository
                .findWithDetailsByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        return adminDetail(order);
    }

    @Override
    @Transactional
    public AdminOrderDetail adminCancel(String code, Long actorId) {
        Order order = orderRepository
                .findForUpdateByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.PAID) {
            cancelPaidOrder(order, actorId, "ADMIN_CANCEL", false);
        } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            transitions.changeStatus(order, OrderStatus.CANCELLED, "ADMIN_CANCEL", actorId, null);
            transitions.closeHold(
                    order.getHoldId(), null, null, HoldStatus.RELEASED, OrderStatus.CANCELLED, "ADMIN_CANCEL", actorId);
        } else {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        return adminDetail(orderRepository.findWithDetailsByCode(code).orElseThrow());
    }

    private AdminOrderDetail adminDetail(Order order) {
        UserBrief customer = userService.briefs(List.of(order.getUserId())).get(order.getUserId());
        List<AdminOrderDetail.EventView> events = orderEventRepository
                .findByOrderIdOrderByCreatedAtAscIdAsc(order.getId())
                .stream()
                .map(e -> new AdminOrderDetail.EventView(
                        e.getType(), e.getFromStatus(), e.getToStatus(), e.getActorId(), e.getData(), e.getCreatedAt()))
                .toList();
        return new AdminOrderDetail(
                views.order(order, clock.instant()), customer, events, paymentQueryService.listByOrder(order.getId()));
    }

    @Override
    @Transactional
    public void cancelGroupOrder(Long orderId, Long actorId) {
        Order order = orderRepository
                .findForUpdateById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        cancelPaidOrder(order, actorId, "GROUP_CANCEL", true);
    }

    @Override
    @Transactional
    public int cancelForShowtime(Long showtimeId, Long actorId) {
        int cancelled = 0;
        for (Long id : orderRepository.findIdsByShowtimeAndStatuses(
                showtimeId, List.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID))) {
            Order order = orderRepository.findForUpdateById(id).orElseThrow();
            if (order.getStatus() == OrderStatus.PAID) {
                cancelPaidOrder(order, actorId, "SHOWTIME_CANCELLED", false);
            } else if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                transitions.changeStatus(order, OrderStatus.CANCELLED, "SHOWTIME_CANCELLED", actorId, null);
            } else {
                continue;
            }
            cancelled++;
        }
        // Lượt giữ còn mở (có hoặc chưa có đơn) → trả ghế, nhóm đang giữ ghế quay về LOCKED
        for (Long holdId : seatHoldRepository.findActiveIdsByShowtime(showtimeId)) {
            transitions.closeHold(
                    holdId, null, null, HoldStatus.RELEASED, OrderStatus.CANCELLED, "SHOWTIME_CANCELLED", actorId);
        }
        return cancelled;
    }

    @Override
    @Transactional
    public RefundView completeRefund(Long refundId, Long actorId, String note) {
        RefundView refund = refundService.markCompleted(refundId, actorId, note);
        orderRepository.findForUpdateById(refund.orderId()).ifPresent(order -> {
            if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.REFUND_PENDING) {
                transitions.changeStatus(order, OrderStatus.REFUNDED, "REFUNDED", actorId, Map.of("amount", refund.amount()));
            }
        });
        return refund;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminRefundView> listRefunds(RefundStatus status, int page, int size) {
        PageResponse<RefundView> refunds = refundService.list(status, page, size);
        Map<Long, Order> orders = orderRepository
                .findAllById(refunds.items().stream().map(RefundView::orderId).toList())
                .stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        return new PageResponse<>(
                refunds.items().stream()
                        .map(r -> {
                            Order o = orders.get(r.orderId());
                            return new AdminRefundView(
                                    r.id(), o.getCode(), o.getUserId(), o.getTotal(), r.amount(), r.reason(), r.status(),
                                    r.provider(), r.note(), r.createdAt(), r.processedAt());
                        })
                        .toList(),
                refunds.page(),
                refunds.size(),
                refunds.totalElements(),
                refunds.totalPages());
    }

    @Override
    @Transactional
    public int completeFinishedOrders() {
        return orderRepository.completeFinished(clock.instant());
    }

    /**
     * Hủy đơn đã thanh toán (BR10): vé → CANCELLED, ghế trả về trống, đơn → CANCELLED và tạo yêu cầu hoàn tiền.
     * Hoàn xong ngay (cổng hỗ trợ hoàn tự động) thì đơn → REFUNDED.
     *
     * @param byPolicy true: áp thời hạn {@code cancel_before_hours} và tỷ lệ {@code refund_percent}; false (rạp hủy
     *     suất): hoàn 100 %, không giới hạn thời gian
     */
    private void cancelPaidOrder(Order order, Long actorId, String reason, boolean byPolicy) {
        Showtime showtime = order.getShowtime();
        Instant now = clock.instant();
        if (byPolicy) {
            Instant deadline = showtime.getStartAt()
                    .minus(Duration.ofHours(settingService.getInt(SettingKey.CANCEL_BEFORE_HOURS)));
            if (now.isAfter(deadline)) {
                throw new BusinessException(ErrorCode.CANCEL_DEADLINE_PASSED);
            }
        }
        if (order.getTickets().stream().anyMatch(t -> t.getStatus() == TicketStatus.USED)) {
            throw new BusinessException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        order.getTickets().forEach(t -> t.setStatus(TicketStatus.CANCELLED));
        // Ghi xuống CSDL trước khi trả ghế: index duy nhất tickets(showtime, seat) bỏ qua vé đã hủy
        orderRepository.flush();
        showtimeSeatService.releaseSold(showtime.getId(), order.getId());

        int percent = byPolicy ? settingService.getInt(SettingKey.REFUND_PERCENT) : 100;
        long amount = order.getTotal() * percent / 100;
        transitions.changeStatus(order, OrderStatus.CANCELLED, reason, actorId, Map.of("refundPercent", percent, "refundAmount", amount));
        RefundView refund = refundService.request(order.getId(), amount, reason, actorId);
        if (refund.status() == RefundStatus.COMPLETED) {
            transitions.changeStatus(order, OrderStatus.REFUNDED, "REFUNDED", null, Map.of("amount", amount));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> listMine(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE));
        return PageResponse.from(
                orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(views::summary));
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyTicketResponse> myTickets(Long userId, boolean past) {
        Instant now = clock.instant();
        List<Ticket> tickets = past
                ? ticketRepository.findPastForUser(userId, now)
                : ticketRepository.findUpcomingForUser(userId, now);
        return tickets.stream().map(views::myTicket).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Long findIdByCode(String code) {
        return orderRepository
                .findByCode(code)
                .map(Order::getId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderSummaryResponse findSummary(Long orderId) {
        return orderRepository
                .findById(orderId)
                .map(views::summary)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderTicket> ticketsOfOrder(Long orderId) {
        return ticketRepository.findByOrderIdOrderByIdAsc(orderId).stream()
                .map(t -> new OrderTicket(
                        t.getId(), t.getCode(), t.getSeatId(), t.getSeatLabel(), t.getHolderUserId(), t.getHolderName()))
                .toList();
    }

    @Override
    @Transactional
    public void assignTicketHolders(Long orderId, Map<Long, TicketHolder> holders) {
        for (Ticket ticket : ticketRepository.findByOrderIdOrderByIdAsc(orderId)) {
            TicketHolder holder = holders.get(ticket.getId());
            if (holder != null) {
                ticket.setHolderUserId(holder.userId());
                ticket.setHolderName(holder.name());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTarget getPayable(Long userId, String code) {
        Order order = orderRepository
                .findByCode(code)
                .filter(o -> o.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT || !order.getExpiresAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.ORDER_NOT_PAYABLE);
        }
        return toTarget(order);
    }

    @Override
    @Transactional
    public PaymentTarget lockForPayment(String code) {
        return orderRepository
                .findForUpdateByCode(code)
                .map(this::toTarget)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    @Override
    @Transactional
    public void applySuccessfulPayment(String code, String provider, String transactionId) {
        Order order = orderRepository
                .findForUpdateByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        Map<String, Object> data = Map.of("provider", provider, "transactionId", transactionId);
        switch (order.getStatus()) {
            // Đơn đã hết hạn/đã hủy nhưng tiền vẫn về: thử nhận lại ghế (BR17)
            case PENDING_PAYMENT, EXPIRED, CANCELLED -> settle(order, data);
            default -> transitions.recordEvent(
                    order, "DUPLICATE_PAYMENT", order.getStatus(), order.getStatus(), null, data);
        }
    }

    @Override
    @Transactional
    public void recordPaymentIssue(String code, String type, Map<String, Object> data) {
        Order order = orderRepository
                .findForUpdateByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
        transitions.recordEvent(order, type, order.getStatus(), order.getStatus(), null, data);
    }

    private void settle(Order order, Map<String, Object> data) {
        SeatHold hold = seatHoldRepository.findForUpdate(order.getHoldId()).orElseThrow();
        List<Long> seatIds =
                order.getItems().stream().map(OrderItem::getSeatId).toList();
        Long showtimeId = order.getShowtime().getId();
        if (showtimeSeatService.claim(showtimeId, seatIds, hold.getId(), order.getId())) {
            Instant now = clock.instant();
            order.setPaidAt(now);
            order.getItems().forEach(item -> {
                Ticket ticket = new Ticket();
                ticket.setShowtimeId(showtimeId);
                ticket.setSeatId(item.getSeatId());
                ticket.setSeatLabel(item.getSeatLabel());
                ticket.setCode(BookingCodes.ticketCode());
                ticket.setHolderUserId(order.getUserId());
                ticket.setCreatedAt(now);
                order.addTicket(ticket);
            });
            hold.setStatus(HoldStatus.CONVERTED);
            transitions.changeStatus(order, OrderStatus.PAID, "PAID", null, data);
        } else {
            if (hold.getStatus() == HoldStatus.ACTIVE) {
                hold.setStatus(HoldStatus.EXPIRED);
                showtimeSeatService.release(showtimeId, hold.getId());
                transitions.publishIfGroupHold(hold);
            }
            transitions.changeStatus(order, OrderStatus.REFUND_PENDING, "PAID_SEATS_UNAVAILABLE", null, data);
            // BR17: hoàn tiền tự động (cổng hỗ trợ) hoặc chờ admin hoàn thủ công
            RefundView refund = refundService.request(order.getId(), order.getTotal(), "SEATS_UNAVAILABLE", null);
            if (refund.status() == RefundStatus.COMPLETED) {
                transitions.changeStatus(order, OrderStatus.REFUNDED, "REFUNDED", null, Map.of("amount", refund.amount()));
            }
        }
    }

    private Optional<OrderResponse> existingOrderOfHold(Long userId, Long holdId) {
        return orderRepository
                .findByHoldId(holdId)
                .filter(order -> order.getUserId().equals(userId))
                .map(order -> views.order(findOwned(userId, order.getCode()), clock.instant()));
    }

    private Order findOwned(Long userId, String code) {
        return orderRepository
                .findWithDetailsByCode(code)
                .filter(order -> order.getUserId().equals(userId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private PaymentTarget toTarget(Order order) {
        return new PaymentTarget(order.getId(), order.getCode(), order.getUserId(), order.getStatus(), order.getTotal());
    }
}
