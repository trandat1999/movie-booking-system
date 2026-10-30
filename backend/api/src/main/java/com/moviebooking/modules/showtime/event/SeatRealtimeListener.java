package com.moviebooking.modules.showtime.event;

import com.moviebooking.infrastructure.realtime.RealtimePublisher;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Đẩy thay đổi trạng thái ghế tới {@code /topic/showtimes/{id}/seats} sau khi commit (PLAN.md 8.3), để người xem
 * không thấy trạng thái của giao dịch đã rollback.
 */
@Component
@RequiredArgsConstructor
public class SeatRealtimeListener {

    private final RealtimePublisher realtimePublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(SeatStatesChangedEvent event) {
        List<Map<String, Object>> seats = event.seats().entrySet().stream()
                .map(e -> Map.<String, Object>of("id", e.getKey(), "status", e.getValue().name()))
                .toList();
        realtimePublisher.toTopic(
                "/topic/showtimes/" + event.showtimeId() + "/seats", Map.of("type", "SEATS_CHANGED", "seats", seats));
    }
}
