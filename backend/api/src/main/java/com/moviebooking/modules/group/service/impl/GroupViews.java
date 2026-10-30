package com.moviebooking.modules.group.service.impl;

import com.moviebooking.common.util.TimeZones;
import com.moviebooking.modules.booking.dto.response.OrderSummaryResponse;
import com.moviebooking.modules.booking.service.HoldService;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.AssignmentView;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.CinemaInfo;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.HoldInfo;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.LockedInfo;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.MemberView;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.MovieInfo;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.OptionView;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.OrderInfo;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.Progress;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse.ShareView;
import com.moviebooking.modules.group.dto.response.GroupShowtimeInfo;
import com.moviebooking.modules.group.entity.BookingGroup;
import com.moviebooking.modules.group.entity.GroupMember;
import com.moviebooking.modules.group.entity.GroupSeatAssignment;
import com.moviebooking.modules.group.entity.GroupShowtimeOption;
import com.moviebooking.modules.group.entity.GroupVote;
import com.moviebooking.modules.group.enums.GroupRole;
import com.moviebooking.modules.group.enums.Rsvp;
import com.moviebooking.modules.group.repository.GroupMemberRepository;
import com.moviebooking.modules.group.repository.GroupSeatAssignmentRepository;
import com.moviebooking.modules.group.repository.GroupShowtimeOptionRepository;
import com.moviebooking.modules.group.repository.GroupVoteRepository;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Dựng snapshot chi tiết nhóm; chạy trong transaction của service gọi nó. */
@Component
@RequiredArgsConstructor
class GroupViews {

    private final GroupMemberRepository memberRepository;
    private final GroupShowtimeOptionRepository optionRepository;
    private final GroupVoteRepository voteRepository;
    private final GroupSeatAssignmentRepository assignmentRepository;
    private final ShowtimeSeatService showtimeSeatService;
    private final HoldService holdService;
    private final OrderService orderService;
    private final Clock clock;

    GroupDetailResponse detail(BookingGroup g, Long viewerId) {
        List<GroupMember> members = memberRepository.findActive(g.getId());
        Map<Long, GroupMember> byUser = members.stream()
                .collect(Collectors.toMap(GroupMember::userId, Function.identity()));
        GroupRole myRole = byUser.containsKey(viewerId) ? byUser.get(viewerId).getRole() : null;

        List<GroupShowtimeOption> options = optionRepository.findByGroup(g.getId());
        Map<Long, List<GroupVote>> votes = voteRepository
                .findByOptions(options.stream().map(GroupShowtimeOption::getId).toList())
                .stream()
                .filter(v -> byUser.containsKey(v.getId().userId()))
                .collect(Collectors.groupingBy(v -> v.getId().optionId()));
        List<OptionView> optionViews = options.stream()
                .map(o -> {
                    List<GroupVote> optionVotes = votes.getOrDefault(o.getId(), List.of());
                    return new OptionView(
                            o.getId(),
                            showtimeInfo(o.getShowtime()),
                            o.getProposedBy(),
                            optionVotes.size(),
                            optionVotes.stream().anyMatch(v -> v.getId().userId().equals(viewerId)),
                            optionVotes.stream()
                                    .map(v -> byUser.get(v.getId().userId()).getUser().getFullName())
                                    .toList());
                })
                .toList();

        Showtime locked = g.getLockedShowtimeId() == null ? null : showtimeSeatService.getShowtime(g.getLockedShowtimeId());
        HoldInfo hold = g.getHoldId() == null
                ? null
                : holdService.findActive(g.getHoldId())
                        .map(h -> new HoldInfo(h.holdId(), h.expiresAt(), h.seatIds()))
                        .orElse(null);
        OrderSummaryResponse order = g.getOrderId() == null ? null : orderService.findSummary(g.getOrderId());

        List<AssignmentView> assignments = new ArrayList<>();
        if (g.getOrderId() != null) {
            Map<Long, GroupSeatAssignment> byTicket = assignmentRepository.findByGroupId(g.getId()).stream()
                    .collect(Collectors.toMap(GroupSeatAssignment::getTicketId, Function.identity()));
            for (OrderService.OrderTicket t : orderService.ticketsOfOrder(g.getOrderId())) {
                GroupSeatAssignment a = byTicket.get(t.id());
                if (a != null) {
                    assignments.add(new AssignmentView(t.code(), t.seatLabel(), a.getUserId(), t.holderName(), a.getGuestLabel()));
                }
            }
        }

        return new GroupDetailResponse(
                g.getId(),
                g.getCode(),
                g.getName(),
                g.getStatus(),
                movieInfo(g.getMovie()),
                g.getCinema() == null ? null : new CinemaInfo(g.getCinema().getId(), g.getCinema().getName()),
                g.getDateFrom(),
                g.getDateTo(),
                g.getRsvpDeadline(),
                g.getLeaderId(),
                myRole,
                members.stream().map(GroupViews::memberView).toList(),
                optionViews,
                locked == null ? null : new LockedInfo(showtimeInfo(locked), g.getLockedTicketCount()),
                hold,
                order == null ? null : new OrderInfo(order.code(), order.status(), order.total()),
                assignments,
                order == null ? List.of() : shares(members, g.getLeaderId(), order.total()),
                progress(members),
                ageWarnings(g.getMovie(), members, locked),
                clock.instant());
    }

    static GroupShowtimeInfo showtimeInfo(Showtime s) {
        return new GroupShowtimeInfo(
                s.getId(),
                s.getStartAt(),
                s.getEndAt(),
                s.getFormat(),
                s.getAudioType(),
                s.getRoom().getCinema().getId(),
                s.getRoom().getCinema().getName(),
                s.getRoom().getName());
    }

    static MovieInfo movieInfo(Movie m) {
        return new MovieInfo(m.getId(), m.getTitle(), m.getSlug(), m.getPosterUrl(), m.getAgeRating(), m.getDurationMin());
    }

    static int ticketCount(Collection<GroupMember> members) {
        return members.stream().mapToInt(GroupMember::tickets).sum();
    }

    static Progress progress(List<GroupMember> members) {
        Map<Rsvp, Long> counts = members.stream().collect(Collectors.groupingBy(GroupMember::getRsvp, Collectors.counting()));
        int pending = counts.getOrDefault(Rsvp.PENDING, 0L).intValue();
        return new Progress(
                members.size(),
                members.size() - pending,
                counts.getOrDefault(Rsvp.YES, 0L).intValue(),
                counts.getOrDefault(Rsvp.NO, 0L).intValue(),
                counts.getOrDefault(Rsvp.MAYBE, 0L).intValue(),
                pending,
                ticketCount(members));
    }

    /**
     * Chia tiền tham khảo (GRP16): theo số người mỗi thành viên "Có" (bản thân + khách); phần lẻ do làm tròn tính cho
     * trưởng nhóm.
     */
    static List<ShareView> shares(List<GroupMember> members, Long leaderId, long total) {
        List<GroupMember> paying = members.stream().filter(m -> m.tickets() > 0).toList();
        int people = paying.stream().mapToInt(GroupMember::tickets).sum();
        if (people == 0) {
            return List.of();
        }
        List<ShareView> shares = new ArrayList<>();
        long assigned = 0;
        for (GroupMember m : paying) {
            long amount = total * m.tickets() / people;
            assigned += amount;
            shares.add(new ShareView(m.userId(), m.getUser().getFullName(), m.tickets(), amount));
        }
        long remainder = total - assigned;
        if (remainder != 0) {
            for (int i = 0; i < shares.size(); i++) {
                ShareView s = shares.get(i);
                if (s.userId().equals(leaderId) || i == shares.size() - 1) {
                    shares.set(i, new ShareView(s.userId(), s.fullName(), s.people(), s.amount() + remainder));
                    break;
                }
            }
        }
        return shares;
    }

    /** Thành viên chưa đủ tuổi xem phim T13/T16/T18 (GRP08), tính tới ngày chiếu đã chốt hoặc hôm nay. */
    List<String> ageWarnings(Movie movie, List<GroupMember> members, Showtime locked) {
        int minAge = switch (movie.getAgeRating()) {
            case T13 -> 13;
            case T16 -> 16;
            case T18 -> 18;
            default -> 0;
        };
        if (minAge == 0) {
            return List.of();
        }
        LocalDate onDate = locked != null
                ? locked.getStartAt().atZone(TimeZones.VIETNAM).toLocalDate()
                : LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        List<String> warnings = new ArrayList<>();
        for (GroupMember m : members) {
            LocalDate dob = m.getUser().getDateOfBirth();
            if (dob != null && m.getRsvp() != Rsvp.NO) {
                int age = Period.between(dob, onDate).getYears();
                if (age < minAge) {
                    warnings.add("%s mới %d tuổi — phim %s chỉ dành cho khán giả từ %d tuổi"
                            .formatted(m.getUser().getFullName(), age, movie.getAgeRating(), minAge));
                }
            }
        }
        return warnings;
    }

    private static MemberView memberView(GroupMember m) {
        return new MemberView(
                m.userId(), m.getUser().getFullName(), m.getRole(), m.getRsvp(), m.getGuestCount(), m.getMaxBudget(),
                m.getJoinedAt());
    }
}
