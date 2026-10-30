package com.moviebooking.modules.group.dto.response;

import java.util.List;

/**
 * Tổng hợp nhu cầu (GRP07, GRP08).
 *
 * @param ticketCount số vé = thành viên "Có" + khách đi kèm (BR18)
 * @param minBudget ngân sách thấp nhất trong các thành viên "Có" đã khai báo
 * @param topOptionId suất được bình chọn nhiều nhất (null nếu chưa có phiếu)
 * @param ageWarnings thành viên chưa đủ tuổi theo phân loại phim
 */
public record GroupSummaryResponse(
        int ticketCount,
        int yes,
        int maybe,
        int no,
        int pending,
        int guests,
        Long minBudget,
        List<OptionSummary> options,
        Long topOptionId,
        List<String> ageWarnings) {

    /**
     * @param availablePlaces số chỗ còn trống của suất
     * @param cluster SAME_ROW: còn cụm liền nhau một hàng đủ cho nhóm; SPLIT: chỉ còn cụm chia nhiều hàng; NONE: không
     *     còn cụm phù hợp; UNAVAILABLE: suất không còn bán
     */
    public record OptionSummary(
            Long optionId, GroupShowtimeInfo showtime, int votes, int availablePlaces, String cluster, Double bestScore) {}
}
