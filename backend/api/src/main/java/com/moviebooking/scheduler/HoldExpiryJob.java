package com.moviebooking.scheduler;

import com.moviebooking.modules.booking.service.HoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dọn lượt giữ ghế đã hết hạn (BR02): trả ghế và chuyển đơn chờ thanh toán sang EXPIRED. Ghế giữ quá hạn vốn đã
 * được coi là trống khi đọc/giữ ghế, job này chỉ để dữ liệu và realtime phản ánh đúng. Mỗi lượt giữ xử lý trong
 * giao dịch riêng để một lỗi không chặn cả lô.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.booking.expiry-job-enabled", havingValue = "true", matchIfMissing = true)
public class HoldExpiryJob {

    private static final int BATCH_SIZE = 200;

    private final HoldService holdService;

    @Scheduled(fixedDelayString = "${app.booking.expiry-interval:PT10S}", initialDelayString = "PT10S")
    @SchedulerLock(name = "holdExpiry", lockAtMostFor = "PT1M")
    public void run() {
        int expired = 0;
        for (Long holdId : holdService.findDueIds(BATCH_SIZE)) {
            try {
                if (holdService.expire(holdId)) {
                    expired++;
                }
            } catch (RuntimeException ex) {
                log.warn("Failed to expire hold {}: {}", holdId, ex.getMessage());
            }
        }
        if (expired > 0) {
            log.info("Expired {} seat hold(s)", expired);
        }
    }
}
