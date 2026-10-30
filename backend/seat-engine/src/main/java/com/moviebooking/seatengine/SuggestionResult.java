package com.moviebooking.seatengine;

import java.util.List;

/**
 * Kết quả đề xuất: các phương án tốt nhất (đa dạng) theo thứ tự điểm giảm dần, hoặc lý do không có phương án.
 *
 * @param reason khác null khi và chỉ khi {@code suggestions} rỗng
 */
public record SuggestionResult(List<Suggestion> suggestions, NoSuggestionReason reason) {

    public SuggestionResult {
        suggestions = List.copyOf(suggestions);
    }

    public boolean found() {
        return !suggestions.isEmpty();
    }

    public enum NoSuggestionReason {
        /** Tổng số chỗ trống (theo loại ghế cho phép) ít hơn số người. */
        NOT_ENOUGH_SEATS,
        /** Đủ chỗ nhưng không có cụm liền nhau thỏa điều kiện (một hàng / số hàng tối đa). */
        NO_CLUSTER
    }
}
