package com.moviebooking.modules.group.event;

import com.moviebooking.infrastructure.realtime.RealtimePublisher;
import com.moviebooking.modules.booking.event.HoldClosedEvent;
import com.moviebooking.modules.booking.event.OrderStatusChangedEvent;
import com.moviebooking.modules.group.service.GroupService;
import com.moviebooking.modules.notification.service.NotificationService;
import com.moviebooking.modules.showtime.event.ShowtimeCancelledEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cầu nối sự kiện của nhóm:
 *
 * <ul>
 *   <li>sự kiện của module đặt vé (lượt giữ đóng, đơn đã thanh toán) → cập nhật nhóm <b>trong cùng</b> transaction;
 *   <li>thay đổi của nhóm → realtime {@code /topic/groups/{id}} và thông báo in-app <b>sau</b> commit.
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GroupEventListener {

    private final GroupService groupService;
    private final RealtimePublisher realtimePublisher;
    private final NotificationService notificationService;

    @EventListener
    public void onHoldClosed(HoldClosedEvent event) {
        groupService.onHoldClosed(event);
    }

    @EventListener
    public void onShowtimeCancelled(ShowtimeCancelledEvent event) {
        groupService.onShowtimeCancelled(event.showtimeId());
    }

    @EventListener(condition = "#event.groupId != null")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        groupService.onOrderStatusChanged(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(GroupChangedEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>(event.data());
        payload.put("type", event.type());
        payload.put("groupId", event.groupId());
        realtimePublisher.toTopic("/topic/groups/" + event.groupId(), payload);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notify(GroupNotifyEvent event) {
        for (Long userId : event.recipients()) {
            try {
                notificationService.notify(userId, event.type(), event.title(), event.body(), "/groups/" + event.groupId());
            } catch (RuntimeException ex) {
                log.error("Failed to notify user {} about group {}: {}", userId, event.groupId(), ex.getMessage());
            }
        }
    }
}
