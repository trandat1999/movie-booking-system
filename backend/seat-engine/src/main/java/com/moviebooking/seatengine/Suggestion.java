package com.moviebooking.seatengine;

import java.util.List;

/**
 * Một phương án cụm ghế.
 *
 * @param seatIds ghế theo thứ tự hàng rồi cột
 * @param rowCount số hàng phương án chiếm
 * @param capacity tổng số chỗ (≥ số người, thừa khi có ghế đôi)
 * @param totalPrice tổng giá các ghế (VND)
 * @param score điểm trong [0, 100], cao hơn là tốt hơn
 * @param explanation giải thích ngắn bằng tiếng Việt cho người dùng
 */
public record Suggestion(
        List<Long> seatIds,
        List<String> labels,
        int rowCount,
        int capacity,
        long totalPrice,
        double score,
        ScoreBreakdown breakdown,
        String explanation) {

    public Suggestion {
        seatIds = List.copyOf(seatIds);
        labels = List.copyOf(labels);
    }
}
