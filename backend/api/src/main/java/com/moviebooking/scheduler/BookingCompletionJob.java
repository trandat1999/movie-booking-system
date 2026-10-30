package com.moviebooking.scheduler;

import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Sau khi suất chiếu kết thúc: đơn PAID và nhóm BOOKED chuyển sang hoàn tất (PLAN.md 6.2, 6.3). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.booking.completion-job-enabled", havingValue = "true", matchIfMissing = true)
public class BookingCompletionJob {

    private final OrderService orderService;
    private final GroupService groupService;

    @Scheduled(fixedDelayString = "${app.booking.completion-interval:PT5M}", initialDelayString = "PT1M")
    @SchedulerLock(name = "bookingCompletion", lockAtMostFor = "PT5M")
    public void run() {
        int orders = orderService.completeFinishedOrders();
        int groups = groupService.completeFinishedGroups();
        if (orders + groups > 0) {
            log.info("Completed {} order(s), {} group(s)", orders, groups);
        }
    }
}
