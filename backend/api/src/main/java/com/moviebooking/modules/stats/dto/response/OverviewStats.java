package com.moviebooking.modules.stats.dto.response;

import java.time.LocalDate;
import java.util.List;

/**
 * Dashboard tổng quan (ADM01) trong khoảng ngày (giờ Việt Nam, gồm cả hai đầu).
 *
 * @param revenue doanh thu thuần: đơn đã thanh toán còn hiệu lực (PAID/COMPLETED), theo ngày thanh toán
 * @param paymentFailureRate giao dịch thất bại / tổng giao dịch nhận từ cổng
 * @param occupancyRate ghế đã bán / ghế bán được của các suất chiếu trong khoảng
 */
public record OverviewStats(
        LocalDate from,
        LocalDate to,
        long revenue,
        long orders,
        long ticketsSold,
        long avgOrderValue,
        long discountTotal,
        long refundTotal,
        long refundCount,
        long paymentAttempts,
        long paymentFailed,
        double paymentFailureRate,
        long expiredOrders,
        long seatsSold,
        long seatsTotal,
        double occupancyRate,
        List<DailyPoint> daily,
        List<Breakdown> topMovies,
        List<Breakdown> cinemas) {

    public record DailyPoint(LocalDate date, long orders, long tickets, long revenue) {}

    public record Breakdown(Long id, String name, long orders, long tickets, long revenue) {}
}
