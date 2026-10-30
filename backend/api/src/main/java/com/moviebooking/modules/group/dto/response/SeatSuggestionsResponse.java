package com.moviebooking.modules.group.dto.response;

import com.moviebooking.seatengine.ScoreBreakdown;
import java.util.List;

/**
 * Phương án cụm ghế cho nhóm (GRP10).
 *
 * @param logId mã lần đề xuất — gửi lại khi giữ ghế để thống kê phương án được chọn
 * @param reason NOT_ENOUGH_SEATS / NO_CLUSTER khi không có phương án
 */
public record SeatSuggestionsResponse(Long logId, int ticketCount, List<SuggestionView> options, String reason) {

    /** @param rank 1 = tốt nhất; 0 = lựa chọn thủ công được chấm điểm */
    public record SuggestionView(
            int rank,
            List<Long> seatIds,
            List<String> labels,
            int rowCount,
            int capacity,
            long totalPrice,
            double score,
            String explanation,
            ScoreBreakdown breakdown) {}
}
