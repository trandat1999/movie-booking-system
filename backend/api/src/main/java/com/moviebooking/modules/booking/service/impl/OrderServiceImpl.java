package com.moviebooking.modules.booking.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.booking.dto.request.CreateOrderRequest;
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
import java.time.Clock;
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
