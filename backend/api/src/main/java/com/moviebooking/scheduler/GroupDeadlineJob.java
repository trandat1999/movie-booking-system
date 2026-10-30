package com.moviebooking.scheduler;

import com.moviebooking.modules.group.service.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Nhắc thành viên chưa phản hồi trước hạn chốt và chuyển nhóm quá hạn chốt sang EXPIRED (GRP09, GRP18). */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.group.deadline-job-enabled", havingValue = "true", matchIfMissing = true)
public class GroupDeadlineJob {

    private final GroupService groupService;

    @Scheduled(fixedDelayString = "${app.group.deadline-interval:PT1M}", initialDelayString = "PT30S")
    @SchedulerLock(name = "groupDeadline", lockAtMostFor = "PT2M")
    public void run() {
        int reminded = groupService.remindPendingMembers();
        int expired = groupService.expireOverdueGroups();
        if (reminded + expired > 0) {
            log.info("Group deadlines: {} reminded, {} expired", reminded, expired);
        }
    }
}
