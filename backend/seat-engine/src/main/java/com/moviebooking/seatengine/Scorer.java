package com.moviebooking.seatengine;

import java.util.List;

/**
 * Hàm chấm điểm (PLAN.md 8.2 bước 4). "Điểm xem đẹp" ở giữa theo chiều ngang và khoảng 2/3 chiều sâu phòng tính
 * từ màn hình (hàng 0).
 */
final class Scorer {

    private final SuggestionRequest request;
    private final double sweetX;
    private final double sweetY;
    private final double halfWidth;
    private final double depthSpan;

    Scorer(SeatLayout layout, SuggestionRequest request) {
        this.request = request;
        this.sweetX = layout.gridCols() / 2.0;
        this.sweetY = layout.gridRows() * 2.0 / 3.0;
        this.halfWidth = Math.max(1, layout.gridCols() / 2.0);
        this.depthSpan = Math.max(1, Math.max(sweetY, layout.gridRows() - sweetY));
    }

    /**
     * @param windows mỗi hàng một cửa sổ, theo thứ tự hàng
     * @param people số người các cửa sổ này phải chứa (cả phương án hoặc phần đã dựng khi beam search)
     */
    Scored score(List<Window> windows, int people) {
        int orphans = 0;
        for (Window w : windows) {
            orphans += w.orphans;
        }
        return score(windows, people, orphans);
    }

    /**
     * @param orphans số ghế lẻ đã tính cho cả tập ghế (khi nhiều cửa sổ cùng một hàng, cộng ghế lẻ từng cửa sổ sẽ
     *     sai)
     */
    Scored score(List<Window> windows, int people, int orphans) {
        int capacity = 0;
        long overBudget = 0;
        double weightedX = 0;
        double weightedY = 0;
        for (Window w : windows) {
            capacity += w.capacity;
            overBudget += w.overBudget;
            weightedX += w.weightedX;
            weightedY += w.capacity * (w.row.gridRow + 0.5);
        }
        double dx = (weightedX / capacity - sweetX) / halfWidth;
        double dy = (weightedY / capacity - sweetY) / depthSpan;
        double center = Math.min(1, Math.sqrt((dx * dx + dy * dy) / 2));

        double misalignment = 0;
        if (windows.size() > 1) {
            double overlap = 0;
            for (int i = 1; i < windows.size(); i++) {
                overlap += windows.get(i).columnOverlapRatio(windows.get(i - 1));
            }
            misalignment = 1 - overlap / (windows.size() - 1);
        }

        int wasted = capacity - people;
        double budget = request.maxPricePerPerson() == null
                ? 0
                : Math.min(1, (double) overBudget / (request.maxPricePerPerson() * people));
        ScoreBreakdown breakdown = new ScoreBreakdown(
                center,
                Math.min(1, (windows.size() - 1) / 2.0),
                misalignment,
                Math.min(1, orphans / 2.0),
                budget,
                Math.min(1, wasted / 2.0),
                orphans,
                wasted);
        ScoringWeights w = request.weights();
        double score = 100
                - w.center() * breakdown.center()
                - w.rows() * breakdown.rows()
                - w.alignment() * breakdown.misalignment()
                - w.orphans() * breakdown.orphans()
                - w.budget() * breakdown.overBudget()
                - w.waste() * breakdown.waste();
        return new Scored(Math.max(0, score), breakdown);
    }

    record Scored(double score, ScoreBreakdown breakdown) {}
}
