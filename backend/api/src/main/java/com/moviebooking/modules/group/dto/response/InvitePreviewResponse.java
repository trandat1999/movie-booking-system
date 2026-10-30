package com.moviebooking.modules.group.dto.response;

import com.moviebooking.modules.group.enums.GroupStatus;

/** Thông tin nhóm trước khi tham gia bằng mã mời. */
public record InvitePreviewResponse(
        Long id,
        String name,
        String movieTitle,
        String posterUrl,
        String leaderName,
        long memberCount,
        GroupStatus status,
        boolean alreadyMember) {}
