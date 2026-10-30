package com.moviebooking.modules.booking.event;

import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.showtime.event.ShowtimeCancelledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Rạp hủy suất → hủy mọi đơn của suất và hoàn 100 % trong cùng transaction (SCHADM03). */
@Component
@RequiredArgsConstructor
public class ShowtimeCancellationListener {

    private final OrderService orderService;

    @EventListener
    public void on(ShowtimeCancelledEvent event) {
        orderService.cancelForShowtime(event.showtimeId(), event.actorId());
    }
}
