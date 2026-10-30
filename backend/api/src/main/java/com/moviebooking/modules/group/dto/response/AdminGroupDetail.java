package com.moviebooking.modules.group.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Chi tiết nhóm cho quản trị: snapshot nhóm, nhật ký thao tác, các lần đề xuất cụm ghế (GRPADM). */
public record AdminGroupDetail(GroupDetailResponse group, List<EventView> events, List<SuggestionLogView> suggestions) {

    public record EventView(String type, Long actorId, String actorName, Map<String, Object> data, Instant createdAt) {}

    public record SuggestionLogView(
            Long id,
            Long showtimeId,
            int partySize,
            boolean contiguous,
            String reason,
            Integer chosenRank,
            int durationMs,
            int optionCount,
            Instant createdAt) {}
}
