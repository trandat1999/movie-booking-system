package com.moviebooking.seatengine;

/**
 * Trọng số của hàm chấm điểm (PLAN.md 8.2): {@code score = 100 − Σ wᵢ · thành phầnᵢ}, mỗi thành phần chuẩn hóa về
 * [0, 1]. Tổng trọng số mặc định = 100 nên điểm luôn trong [0, 100].
 *
 * @param center khoảng cách tâm cụm tới "điểm xem đẹp"
 * @param rows số hàng phải chia
 * @param alignment độ lệch cột giữa các hàng
 * @param orphans số ghế lẻ tạo ra (BR14)
 * @param budget phần giá vượt ngân sách
 * @param waste chỗ thừa do ghế đôi
 */
public record ScoringWeights(double center, double rows, double alignment, double orphans, double budget, double waste) {

    /**
     * Mặc định: ngồi chung một hàng được ưu tiên ngang vị trí trung tâm (chia 2 hàng −15, 3 hàng −30); tinh chỉnh
     * theo thực nghiệm M5 ({@code docs/experiments/README.md}).
     */
    public static final ScoringWeights DEFAULT = new ScoringWeights(30, 30, 10, 15, 5, 10);

    public ScoringWeights {
        if (center < 0 || rows < 0 || alignment < 0 || orphans < 0 || budget < 0 || waste < 0) {
            throw new IllegalArgumentException("Weights must not be negative");
        }
    }
}
