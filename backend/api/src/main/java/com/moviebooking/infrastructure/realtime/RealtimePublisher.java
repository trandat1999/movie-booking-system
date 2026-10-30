package com.moviebooking.infrastructure.realtime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Đẩy thông điệp STOMP tới client (PLAN.md 8.3). Lỗi gửi không làm hỏng nghiệp vụ đã commit. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void toTopic(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
        } catch (RuntimeException ex) {
            log.warn("Realtime publish to {} failed: {}", destination, ex.getMessage());
        }
    }

    /** Gửi tới {@code /user/queue/...} của một người dùng (tên principal = id người dùng). */
    public void toUser(Long userId, String destination, Object payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(userId), destination, payload);
        } catch (RuntimeException ex) {
            log.warn("Realtime publish to user {} failed: {}", userId, ex.getMessage());
        }
    }
}
