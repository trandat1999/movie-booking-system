package com.moviebooking.modules.group.dto.response;

import com.moviebooking.modules.group.enums.GroupStatus;
import com.moviebooking.modules.user.dto.response.UserBrief;
import java.time.Instant;

/** Một dòng trong danh sách nhóm của trang quản trị (GRPADM). */
public record AdminGroupRow(
        Long id,
        String code,
        String name,
        GroupStatus status,
        String movieTitle,
        UserBrief leader,
        long memberCount,
        Integer lockedTicketCount,
        Instant rsvpDeadline,
        Instant createdAt) {}
