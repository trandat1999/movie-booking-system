package com.moviebooking.modules.notification.event;

import com.moviebooking.infrastructure.realtime.RealtimePublisher;
import com.moviebooking.modules.booking.event.OrderStatusChangedEvent;
import com.moviebooking.modules.notification.service.NotificationService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sau khi đơn đổi trạng thái (đã commit): đẩy trạng thái mới tới {@code /user/queue/orders}, tạo thông báo trong
 * ứng dụng và gửi email xác nhận. Lỗi ở đây không ảnh hưởng đơn đã commit.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderNotificationListener {

    private final NotificationService notificationService;
    private final RealtimePublisher realtimePublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(OrderStatusChangedEvent event) {
        realtimePublisher.toUser(
                event.userId(),
                "/queue/orders",
                Map.of("orderCode", event.orderCode(), "status", event.to().name()));
        String link = "/orders/" + event.orderCode();
        try {
            switch (event.to()) {
                case PAID -> {
                    notificationService.notify(
                            event.userId(),
                            "ORDER_PAID",
                            "Đặt vé thành công",
                            "Đơn " + event.orderCode() + " đã được thanh toán. Vé điện tử đã sẵn sàng.",
                            link);
                    notificationService.sendOrderPaidEmail(event.userId(), event.orderCode());
                }
                case REFUND_PENDING -> notificationService.notify(
                        event.userId(),
                        "ORDER_REFUND_PENDING",
                        "Đơn hàng sẽ được hoàn tiền",
                        "Thanh toán của đơn " + event.orderCode()
                                + " đến sau khi ghế đã được bán cho người khác. Chúng tôi sẽ hoàn tiền cho bạn.",
                        link);
                case EXPIRED -> notificationService.notify(
                        event.userId(),
                        "ORDER_EXPIRED",
                        "Đơn hàng đã hết hạn",
                        "Đơn " + event.orderCode() + " đã hết thời gian giữ ghế và chưa được thanh toán.",
                        link);
                default -> {
                    // Đơn do chính người dùng hủy: không cần thông báo
                }
            }
        } catch (RuntimeException ex) {
            log.error("Failed to notify order {} -> {}: {}", event.orderCode(), event.to(), ex.getMessage());
        }
    }
}
