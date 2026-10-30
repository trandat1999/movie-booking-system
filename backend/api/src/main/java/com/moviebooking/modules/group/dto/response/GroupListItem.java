package com.moviebooking.modules.group.dto.response;

import com.moviebooking.modules.group.enums.GroupRole;
import com.moviebooking.modules.group.enums.GroupStatus;
import java.time.Instant;

/** Một dòng trong "Nhóm của tôi" (GRP20). */
public record GroupListItem(
        Long id,
        String name,
        GroupStatus status,
        String movieTitle,
        String posterUrl,
        GroupRole myRole,
        long memberCount,
        Instant rsvpDeadline,
        Instant createdAt) {}
