package com.moviebooking.modules.stats.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Thống kê đặt vé nhóm (ADM02) cho các nhóm tạo trong khoảng ngày.
 *
 * @param conversionRate nhóm đặt vé thành công (BOOKED/COMPLETED) / tổng nhóm
 * @param avgHoursToBook thời gian trung bình từ tạo nhóm tới thanh toán (giờ)
 */
public record GroupStats(
        LocalDate from,
        LocalDate to,
        long totalGroups,
        Map<String, Long> byStatus,
        long booked,
        double conversionRate,
        double avgTickets,
        double avgMembers,
        Double avgHoursToBook,
        SuggestionStats suggestions,
        List<Bucket> sizeBuckets,
        List<Bucket> failures) {

    /**
     * @param contiguousRate tỷ lệ lần đề xuất có phương án tốt nhất ngồi liền nhau cùng hàng
     * @param noCluster số lần không có phương án (không đủ ghế / không còn cụm)
     * @param topChoiceRate trong các lần giữ ghế: tỷ lệ chọn đúng phương án #1
     * @param manualRate trong các lần giữ ghế: tỷ lệ trưởng nhóm tự chọn tay
     */
    public record SuggestionStats(
            long count,
            double contiguousRate,
            long noCluster,
            double avgDurationMs,
            long holds,
            double topChoiceRate,
            double manualRate) {}

    public record Bucket(String label, long count) {}
}
