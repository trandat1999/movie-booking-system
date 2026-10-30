package com.moviebooking.modules.group.dto.response;

import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.group.enums.GroupRole;
import com.moviebooking.modules.group.enums.GroupStatus;
import com.moviebooking.modules.group.enums.Rsvp;
import com.moviebooking.modules.movie.enums.AgeRating;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Toàn bộ trạng thái nhóm cho trang chi tiết (snapshot — client tải lại sau mỗi sự kiện realtime).
 *
 * @param locked khác null từ khi chốt
 * @param hold khác null khi đang giữ ghế (HOLDING)
 * @param order khác null khi đã tạo đơn
 * @param shares số tiền chia tham khảo cho từng thành viên (GRP16), khi đã có đơn
 */
public record GroupDetailResponse(
        Long id,
        String code,
        String name,
        GroupStatus status,
        MovieInfo movie,
        CinemaInfo cinema,
        LocalDate dateFrom,
        LocalDate dateTo,
        Instant rsvpDeadline,
        Long leaderId,
        GroupRole myRole,
        List<MemberView> members,
        List<OptionView> options,
        LockedInfo locked,
        HoldInfo hold,
        OrderInfo order,
        List<AssignmentView> assignments,
        List<ShareView> shares,
        Progress progress,
        List<String> ageWarnings,
        Instant serverTime) {

    public record MovieInfo(Long id, String title, String slug, String posterUrl, AgeRating ageRating, int durationMin) {}

    public record CinemaInfo(Long id, String name) {}

    public record MemberView(
            Long userId, String fullName, GroupRole role, Rsvp rsvp, int guestCount, Long maxBudget, Instant joinedAt) {}

    public record OptionView(
            Long id, GroupShowtimeInfo showtime, Long proposedBy, int votes, boolean votedByMe, List<String> voters) {}

    public record LockedInfo(GroupShowtimeInfo showtime, int ticketCount) {}

    public record HoldInfo(Long holdId, Instant expiresAt, List<Long> seatIds) {}

    public record OrderInfo(String code, OrderStatus status, long total) {}

    /** @param guestLabel khác null nếu là vé của khách đi kèm */
    public record AssignmentView(String ticketCode, String seatLabel, Long userId, String holderName, String guestLabel) {}

    /** @param people số người (bản thân + khách) */
    public record ShareView(Long userId, String fullName, int people, long amount) {}

    public record Progress(int members, int responded, int yes, int no, int maybe, int pending, int ticketCount) {}
}
