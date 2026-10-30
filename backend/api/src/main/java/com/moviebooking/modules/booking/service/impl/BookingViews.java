package com.moviebooking.modules.booking.service.impl;

import com.moviebooking.infrastructure.qr.QrCodeGenerator;
import com.moviebooking.infrastructure.qr.TicketSigner;
import com.moviebooking.modules.booking.dto.response.BookingShowtimeInfo;
import com.moviebooking.modules.booking.dto.response.MyTicketResponse;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.dto.response.OrderSummaryResponse;
import com.moviebooking.modules.booking.dto.response.SeatLine;
import com.moviebooking.modules.booking.dto.response.TicketResponse;
import com.moviebooking.modules.booking.entity.Order;
import com.moviebooking.modules.booking.entity.Ticket;
import com.moviebooking.modules.booking.enums.TicketStatus;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.payment.service.RefundService;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Dựng DTO đơn/vé; QR chứa mã vé đã ký HMAC để quầy soát vé xác minh (BR07). */
@Component
@RequiredArgsConstructor
class BookingViews {

    private final TicketSigner ticketSigner;
    private final QrCodeGenerator qrCodeGenerator;
    private final RefundService refundService;
    private final SettingService settingService;

    BookingShowtimeInfo showtime(Showtime showtime) {
        return new BookingShowtimeInfo(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getMovie().getPosterUrl(),
                showtime.getMovie().getAgeRating(),
                showtime.getStartAt(),
                showtime.getEndAt(),
                showtime.getFormat(),
                showtime.getAudioType(),
                showtime.getRoom().getCinema().getId(),
                showtime.getRoom().getCinema().getName(),
                showtime.getRoom().getCinema().getAddress(),
                showtime.getRoom().getName());
    }

    OrderResponse order(Order order, Instant now) {
        return new OrderResponse(
                order.getCode(),
                order.getStatus(),
                showtime(order.getShowtime()),
                order.getItems().stream()
                        .map(item -> new SeatLine(
                                item.getSeatId(), item.getSeatLabel(), item.getSeatTypeCode(), item.getPrice()))
                        .toList(),
                order.getSubtotal(),
                order.getDiscount(),
                order.getPromotionCode(),
                order.getTotal(),
                order.getExpiresAt(),
                order.getPaidAt(),
                order.getCancelledAt(),
                order.getCreatedAt(),
                order.getTickets().stream()
                        .map(ticket -> new TicketResponse(
                                ticket.getCode(), ticket.getSeatLabel(), ticket.getStatus(), qr(ticket)))
                        .toList(),
                order.getGroupId(),
                order.getStatus() == OrderStatus.PAID
                        ? order.getShowtime().getStartAt()
                                .minus(Duration.ofHours(settingService.getInt(SettingKey.CANCEL_BEFORE_HOURS)))
                        : null,
                order.getStatus() == OrderStatus.PAID
                        ? qrCodeGenerator.toDataUri(ticketSigner.sign(order.getCode()))
                        : null,
                refundService.findByOrder(order.getId())
                        .map(r -> new OrderResponse.RefundInfo(r.amount(), r.status(), r.processedAt()))
                        .orElse(null),
                now);
    }

    OrderSummaryResponse summary(Order order) {
        return new OrderSummaryResponse(
                order.getCode(),
                order.getStatus(),
                showtime(order.getShowtime()),
                order.getItems().size(),
                order.getTotal(),
                order.getCreatedAt());
    }

    MyTicketResponse myTicket(Ticket ticket) {
        Order order = ticket.getOrder();
        return new MyTicketResponse(
                ticket.getCode(),
                order.getCode(),
                ticket.getSeatLabel(),
                ticket.getStatus(),
                showtime(order.getShowtime()),
                qr(ticket));
    }

    private String qr(Ticket ticket) {
        return ticket.getStatus() == TicketStatus.VALID
                ? qrCodeGenerator.toDataUri(ticketSigner.sign(ticket.getCode()))
                : null;
    }
}
