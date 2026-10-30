package com.moviebooking.modules.showtime.event;

import com.moviebooking.modules.cinema.event.SeatAvailabilityChangedEvent;
import com.moviebooking.modules.showtime.repository.ShowtimeSeatRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Ghế bị tắt/bật bán → cập nhật trạng thái ghế ở các suất chưa bắt đầu (cùng transaction). */
@Component
@RequiredArgsConstructor
public class SeatAvailabilityListener {

    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final Clock clock;

    @EventListener
    public void on(SeatAvailabilityChangedEvent event) {
        showtimeSeatRepository.setSeatBlocked(event.seatId(), !event.active(), clock.instant());
    }
}
