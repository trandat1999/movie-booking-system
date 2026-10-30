package com.moviebooking.modules.booking.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.infrastructure.qr.TicketSigner;
import com.moviebooking.modules.booking.dto.response.CheckinResponse;
import com.moviebooking.modules.booking.entity.Order;
import com.moviebooking.modules.booking.entity.Ticket;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.booking.enums.TicketStatus;
import com.moviebooking.modules.booking.repository.OrderRepository;
import com.moviebooking.modules.booking.repository.TicketRepository;
import com.moviebooking.modules.booking.service.CheckinService;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckinServiceImpl implements CheckinService {

    private final TicketSigner ticketSigner;
    private final TicketRepository ticketRepository;
    private final OrderRepository orderRepository;
    private final BookingViews views;
    private final Clock clock;

    @Override
    @Transactional
    public CheckinResponse checkin(Long staffId, String payload) {
        String code = ticketSigner
                .verify(payload.trim())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TICKET_QR));
        Instant now = clock.instant();
        List<Ticket> targets;
        Order order;
        if (code.startsWith("T")) {
            Ticket ticket = ticketRepository
                    .findForUpdateByCode(code)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
            order = ticket.getOrder();
            ensureUsable(order, now);
            if (ticket.getStatus() == TicketStatus.USED) {
                throw alreadyUsed(ticket);
            }
            if (ticket.getStatus() != TicketStatus.VALID) {
                throw new BusinessException(ErrorCode.TICKET_NOT_VALID);
            }
            targets = List.of(ticket);
        } else {
            // QR của cả đơn: soát mọi vé còn hiệu lực một lần (nhóm vào rạp cùng nhau)
            order = orderRepository
                    .findForUpdateByCode(code)
                    .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND));
            ensureUsable(order, now);
            targets = order.getTickets().stream().filter(t -> t.getStatus() == TicketStatus.VALID).toList();
            if (targets.isEmpty()) {
                Ticket used = order.getTickets().stream()
                        .filter(t -> t.getStatus() == TicketStatus.USED)
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_VALID));
                throw alreadyUsed(used);
            }
        }
        for (Ticket t : targets) {
            t.setStatus(TicketStatus.USED);
            t.setCheckedInAt(now);
            t.setCheckedInBy(staffId);
        }
        return new CheckinResponse(
                order.getCode(),
                views.showtime(order.getShowtime()),
                targets.size(),
                order.getTickets().stream()
                        .map(t -> new CheckinResponse.CheckinTicket(
                                t.getCode(), t.getSeatLabel(), t.getHolderName(), t.getStatus(), t.getCheckedInAt()))
                        .toList());
    }

    /** Đơn đã thanh toán, suất chưa hủy, đang trong khung giờ soát vé. */
    private static void ensureUsable(Order order, Instant now) {
        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TICKET_NOT_VALID);
        }
        Showtime showtime = order.getShowtime();
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.TICKET_NOT_VALID);
        }
        if (now.isBefore(showtime.getStartAt().minus(Duration.ofMinutes(OPENS_BEFORE_MIN)))
                || now.isAfter(showtime.getEndAt())) {
            throw new BusinessException(ErrorCode.CHECKIN_NOT_OPEN);
        }
    }

    private static BusinessException alreadyUsed(Ticket ticket) {
        return new BusinessException(
                ErrorCode.TICKET_ALREADY_USED,
                "Vé ghế " + ticket.getSeatLabel() + " đã được soát trước đó",
                Map.of("seatLabel", ticket.getSeatLabel(), "checkedInAt", ticket.getCheckedInAt().toString()));
    }
}
