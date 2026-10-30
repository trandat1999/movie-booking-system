package com.moviebooking.seatengine;

/**
 * Các thành phần đã chuẩn hóa về [0, 1] (0 = tốt nhất) và số liệu thô dùng để giải thích phương án.
 *
 * @param center khoảng cách tâm cụm tới điểm xem đẹp
 * @param rows (số cụm − 1) / 2, tối đa 1 — với phương án do engine đề xuất, mỗi hàng đúng một cụm
 * @param misalignment 1 − độ chồng cột trung bình giữa hai hàng kề nhau (phần giao / bề rộng cửa sổ hẹp hơn)
 * @param orphans số ghế lẻ / 2, tối đa 1
 * @param overBudget phần vượt ngân sách / tổng ngân sách
 * @param waste chỗ thừa / 2, tối đa 1
 * @param orphanSeats số ghế lẻ tạo ra
 * @param wastedPlaces số chỗ thừa (do ghế đôi)
 */
public record ScoreBreakdown(
        double center,
        double rows,
        double misalignment,
        double orphans,
        double overBudget,
        double waste,
        int orphanSeats,
        int wastedPlaces) {}
