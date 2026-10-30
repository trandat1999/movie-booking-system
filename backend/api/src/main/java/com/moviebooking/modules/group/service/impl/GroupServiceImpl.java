package com.moviebooking.modules.group.service.impl;

import com.moviebooking.common.util.TimeZones;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.booking.dto.request.CreateOrderRequest;
import com.moviebooking.modules.booking.dto.response.OrderResponse;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.booking.event.HoldClosedEvent;
import com.moviebooking.modules.booking.event.OrderStatusChangedEvent;
import com.moviebooking.modules.booking.service.HoldService;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.cinema.service.CinemaService;
import com.moviebooking.modules.group.dto.request.AssignmentRequest;
import com.moviebooking.modules.group.dto.request.CreateGroupRequest;
import com.moviebooking.modules.group.dto.request.GroupHoldRequest;
import com.moviebooking.modules.group.dto.request.LockGroupRequest;
import com.moviebooking.modules.group.dto.request.RsvpRequest;
import com.moviebooking.modules.group.dto.request.SeatSuggestionRequest;
import com.moviebooking.modules.group.dto.request.UpdateGroupRequest;
import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.group.dto.response.AdminGroupDetail;
import com.moviebooking.modules.group.dto.response.AdminGroupRow;
import com.moviebooking.modules.group.dto.response.GroupDetailResponse;
import com.moviebooking.modules.user.dto.response.UserBrief;
import org.springframework.data.jpa.domain.Specification;
import com.moviebooking.modules.group.dto.response.GroupListItem;
import com.moviebooking.modules.group.dto.response.GroupSummaryResponse;
import com.moviebooking.modules.group.dto.response.GroupSummaryResponse.OptionSummary;
import com.moviebooking.modules.group.dto.response.InvitePreviewResponse;
import com.moviebooking.modules.group.dto.response.SeatSuggestionsResponse;
import com.moviebooking.modules.group.dto.response.SeatSuggestionsResponse.SuggestionView;
import com.moviebooking.modules.group.entity.BookingGroup;
import com.moviebooking.modules.group.entity.GroupEvent;
import com.moviebooking.modules.group.entity.GroupMember;
import com.moviebooking.modules.group.entity.GroupSeatAssignment;
import com.moviebooking.modules.group.entity.GroupShowtimeOption;
import com.moviebooking.modules.group.entity.GroupVote;
import com.moviebooking.modules.group.entity.SeatSuggestionLog;
import com.moviebooking.modules.group.enums.GroupRole;
import com.moviebooking.modules.group.enums.GroupStatus;
import com.moviebooking.modules.group.enums.Rsvp;
import com.moviebooking.modules.group.event.GroupChangedEvent;
import com.moviebooking.modules.group.event.GroupNotifyEvent;
import com.moviebooking.modules.group.repository.BookingGroupRepository;
import com.moviebooking.modules.group.repository.GroupEventRepository;
import com.moviebooking.modules.group.repository.GroupMemberRepository;
import com.moviebooking.modules.group.repository.GroupSeatAssignmentRepository;
import com.moviebooking.modules.group.repository.GroupShowtimeOptionRepository;
import com.moviebooking.modules.group.repository.GroupVoteRepository;
import com.moviebooking.modules.group.repository.SeatSuggestionLogRepository;
import com.moviebooking.modules.group.service.GroupService;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.service.MovieService;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import com.moviebooking.modules.user.service.UserService;
import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.Suggestion;
import com.moviebooking.seatengine.SuggestionRequest;
import com.moviebooking.seatengine.SuggestionResult;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupServiceImpl implements GroupService {

    private static final char[] CODE_ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final BookingGroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final GroupShowtimeOptionRepository optionRepository;
    private final GroupVoteRepository voteRepository;
    private final GroupSeatAssignmentRepository assignmentRepository;
    private final GroupEventRepository eventRepository;
    private final SeatSuggestionLogRepository suggestionLogRepository;
    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final UserService userService;
    private final ShowtimeSeatService showtimeSeatService;
    private final HoldService holdService;
    private final OrderService orderService;
    private final SettingService settingService;
    private final GroupSeatPlanner planner;
    private final GroupViews views;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    // ===================================================================== Nhóm & thành viên

    @Override
    @Transactional
    public GroupDetailResponse create(Long userId, CreateGroupRequest request) {
        checkActiveGroupLimit(userId);
        Movie movie = movieService.getEntity(request.movieId());
        if (movie.getStatus() == MovieStatus.ENDED) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_SCHEDULABLE);
        }
        BookingGroup group = new BookingGroup();
        group.setCode(newCode());
        group.setLeaderId(userId);
        group.setMovie(movie);
        applyEditable(group, request.name(), request.cinemaId(), request.dateFrom(), request.dateTo(), request.rsvpDeadline());
        groupRepository.saveAndFlush(group);

        GroupMember leader = new GroupMember();
        leader.setGroupId(group.getId());
        leader.setUser(userService.getWithRoles(userId));
        leader.setRole(GroupRole.LEADER);
        leader.setRsvp(Rsvp.YES);
        leader.setJoinedAt(clock.instant());
        memberRepository.save(leader);
        record(group, "CREATED", userId, Map.of("name", group.getName()));
        return views.detail(group, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupDetailResponse get(Long userId, Long groupId) {
        BookingGroup group = groupRepository
                .findWithDetailsById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        requireMember(group, userId);
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse update(Long userId, Long groupId, UpdateGroupRequest request) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireOpen(group);
        applyEditable(group, request.name(), request.cinemaId(), request.dateFrom(), request.dateTo(), request.rsvpDeadline());
        record(group, "UPDATED", userId, Map.of());
        changed(group, "UPDATED", Map.of());
        return views.detail(group, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public InvitePreviewResponse previewInvite(Long userId, String code) {
        BookingGroup group = byCode(code);
        List<GroupMember> members = memberRepository.findActive(group.getId());
        String leaderName = members.stream()
                .filter(m -> m.getRole() == GroupRole.LEADER)
                .map(m -> m.getUser().getFullName())
                .findFirst()
                .orElse("");
        return new InvitePreviewResponse(
                group.getId(),
                group.getName(),
                group.getMovie().getTitle(),
                group.getMovie().getPosterUrl(),
                leaderName,
                members.size(),
                group.getStatus(),
                members.stream().anyMatch(m -> m.userId().equals(userId)));
    }

    @Override
    @Transactional
    public GroupDetailResponse join(Long userId, String code) {
        BookingGroup group = groupRepository
                .findForUpdate(byCode(code).getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_INVALID));
        requireOpen(group);
        GroupMember existing = memberRepository.findByGroupAndUser(group.getId(), userId).orElse(null);
        if (existing != null && existing.isActive()) {
            throw new BusinessException(ErrorCode.ALREADY_GROUP_MEMBER);
        }
        List<GroupMember> members = memberRepository.findActive(group.getId());
        int people = members.stream().mapToInt(m -> 1 + m.getGuestCount()).sum();
        if (people + 1 > settingService.getInt(SettingKey.GROUP_MAX_SIZE)) {
            throw new BusinessException(ErrorCode.GROUP_FULL);
        }
        checkActiveGroupLimit(userId);

        GroupMember member = existing != null ? existing : new GroupMember();
        if (existing == null) {
            member.setGroupId(group.getId());
            member.setUser(userService.getWithRoles(userId));
        }
        member.setRole(GroupRole.MEMBER);
        member.setRsvp(Rsvp.PENDING);
        member.setGuestCount(0);
        member.setMaxBudget(null);
        member.setJoinedAt(clock.instant());
        member.setLeftAt(null);
        memberRepository.save(member);

        record(group, "MEMBER_JOINED", userId, Map.of());
        changed(group, "MEMBER_JOINED", Map.of("userId", userId));
        notifyUsers(group, List.of(group.getLeaderId()), "GROUP_MEMBER_JOINED", "Thành viên mới",
                member.getUser().getFullName() + " đã tham gia nhóm " + group.getName() + ".");
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public void leave(Long userId, Long groupId) {
        BookingGroup group = lockGroup(groupId);
        GroupMember member = requireMember(group, userId);
        if (member.getRole() == GroupRole.LEADER) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        requireStatus(group, GroupStatus.OPEN, GroupStatus.LOCKED);
        deactivate(group, member);
        record(group, "MEMBER_LEFT", userId, Map.of());
        changed(group, "MEMBER_LEFT", Map.of("userId", userId));
    }

    @Override
    @Transactional
    public GroupDetailResponse removeMember(Long userId, Long groupId, Long memberUserId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.OPEN, GroupStatus.LOCKED);
        GroupMember member = memberRepository
                .findByGroupAndUser(groupId, memberUserId)
                .filter(GroupMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (member.getRole() == GroupRole.LEADER) {
            throw new BusinessException(ErrorCode.LEADER_CANNOT_LEAVE);
        }
        deactivate(group, member);
        record(group, "MEMBER_REMOVED", userId, Map.of("userId", memberUserId));
        changed(group, "MEMBER_LEFT", Map.of("userId", memberUserId));
        notifyUsers(group, List.of(memberUserId), "GROUP_REMOVED", "Bạn đã bị mời ra khỏi nhóm",
                "Trưởng nhóm đã mời bạn ra khỏi nhóm " + group.getName() + ".");
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse transferLeader(Long userId, Long groupId, Long newLeaderId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.OPEN, GroupStatus.LOCKED);
        GroupMember next = memberRepository
                .findByGroupAndUser(groupId, newLeaderId)
                .filter(GroupMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (next.getRole() != GroupRole.LEADER) {
            GroupMember current = requireMember(group, userId);
            current.setRole(GroupRole.MEMBER);
            next.setRole(GroupRole.LEADER);
            group.setLeaderId(newLeaderId);
            record(group, "LEADER_TRANSFERRED", userId, Map.of("to", newLeaderId));
            changed(group, "UPDATED", Map.of());
            notifyUsers(group, List.of(newLeaderId), "GROUP_LEADER", "Bạn là trưởng nhóm mới",
                    "Bạn được chuyển quyền trưởng nhóm " + group.getName() + ".");
        }
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse regenerateCode(Long userId, Long groupId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        group.setCode(newCode());
        record(group, "INVITE_CODE_REGENERATED", userId, Map.of());
        return views.detail(group, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupListItem> listMine(Long userId) {
        List<BookingGroup> groups = groupRepository.findForMember(userId);
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Long, Long> counts = new HashMap<>();
        memberRepository
                .countActiveByGroup(groups.stream().map(BookingGroup::getId).toList())
                .forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        return groups.stream()
                .map(g -> new GroupListItem(
                        g.getId(),
                        g.getName(),
                        g.getStatus(),
                        g.getMovie().getTitle(),
                        g.getMovie().getPosterUrl(),
                        g.getLeaderId().equals(userId) ? GroupRole.LEADER : GroupRole.MEMBER,
                        counts.getOrDefault(g.getId(), 0L),
                        g.getRsvpDeadline(),
                        g.getCreatedAt()))
                .toList();
    }

    // ===================================================================== Phản hồi, bình chọn, chốt

    @Override
    @Transactional
    public GroupDetailResponse rsvp(Long userId, Long groupId, RsvpRequest request) {
        BookingGroup group = lockGroup(groupId);
        GroupMember member = requireMember(group, userId);
        requireOpen(group);
        int guests = request.rsvp() == Rsvp.YES || request.rsvp() == Rsvp.MAYBE ? request.guestCount() : 0;
        int others = memberRepository.findActive(groupId).stream()
                .filter(m -> !m.getId().equals(member.getId()))
                .mapToInt(m -> 1 + m.getGuestCount())
                .sum();
        if (others + 1 + guests > settingService.getInt(SettingKey.GROUP_MAX_SIZE)) {
            throw new BusinessException(ErrorCode.GROUP_FULL);
        }
        member.setRsvp(request.rsvp());
        member.setGuestCount(guests);
        member.setMaxBudget(request.maxBudget());
        record(group, "RSVP", userId, Map.of("rsvp", request.rsvp().name(), "guests", guests));
        changed(group, "RSVP_UPDATED", Map.of("userId", userId));
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse proposeOption(Long userId, Long groupId, Long showtimeId) {
        BookingGroup group = lockGroup(groupId);
        requireMember(group, userId);
        requireOpen(group);
        Showtime showtime = eligibleShowtime(group, showtimeId);
        if (!optionRepository.existsByGroupIdAndShowtimeId(groupId, showtimeId)) {
            GroupShowtimeOption option = new GroupShowtimeOption();
            option.setGroupId(groupId);
            option.setShowtime(showtime);
            option.setProposedBy(userId);
            option.setCreatedAt(clock.instant());
            optionRepository.saveAndFlush(option);
            // Người đề xuất mặc định bầu cho suất mình đề xuất
            saveVote(option.getId(), userId);
            record(group, "OPTION_PROPOSED", userId, Map.of("showtimeId", showtimeId));
            changed(group, "VOTE_UPDATED", Map.of());
        }
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse removeOption(Long userId, Long groupId, Long optionId) {
        BookingGroup group = lockGroup(groupId);
        requireMember(group, userId);
        requireOpen(group);
        GroupShowtimeOption option = optionRepository
                .findByIdAndGroupId(optionId, groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
        if (!option.getProposedBy().equals(userId) && !group.getLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_GROUP_LEADER);
        }
        optionRepository.delete(option);
        record(group, "OPTION_REMOVED", userId, Map.of("showtimeId", option.getShowtime().getId()));
        changed(group, "VOTE_UPDATED", Map.of());
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse vote(Long userId, Long groupId, Long optionId, boolean voted) {
        BookingGroup group = lockGroup(groupId);
        requireMember(group, userId);
        requireOpen(group);
        optionRepository
                .findByIdAndGroupId(optionId, groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OPTION_NOT_FOUND));
        GroupVote.Key key = new GroupVote.Key(optionId, userId);
        if (voted && !voteRepository.existsById(key)) {
            saveVote(optionId, userId);
        } else if (!voted) {
            voteRepository.deleteById(key);
        }
        changed(group, "VOTE_UPDATED", Map.of());
        return views.detail(group, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupSummaryResponse summary(Long userId, Long groupId) {
        BookingGroup group = groupRepository
                .findWithDetailsById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        requireMember(group, userId);
        List<GroupMember> members = memberRepository.findActive(groupId);
        Set<Long> memberIds = members.stream().map(GroupMember::userId).collect(Collectors.toSet());
        int ticketCount = GroupViews.ticketCount(members);
        Long minBudget = members.stream()
                .filter(m -> m.getRsvp() == Rsvp.YES && m.getMaxBudget() != null)
                .map(GroupMember::getMaxBudget)
                .min(Long::compare)
                .orElse(null);

        List<GroupShowtimeOption> options = optionRepository.findByGroup(groupId);
        Map<Long, Long> votes = voteRepository.findByOptions(options.stream().map(GroupShowtimeOption::getId).toList())
                .stream()
                .filter(v -> memberIds.contains(v.getId().userId()))
                .collect(Collectors.groupingBy(v -> v.getId().optionId(), Collectors.counting()));
        int partySize = Math.max(1, ticketCount);
        List<OptionSummary> summaries = options.stream()
                .map(o -> optionSummary(o, votes.getOrDefault(o.getId(), 0L).intValue(), partySize, minBudget))
                .toList();
        Long top = summaries.stream()
                .filter(s -> s.votes() > 0)
                .max(Comparator.comparingInt(OptionSummary::votes))
                .map(OptionSummary::optionId)
                .orElse(null);
        Map<Rsvp, Long> counts = members.stream().collect(Collectors.groupingBy(GroupMember::getRsvp, Collectors.counting()));
        return new GroupSummaryResponse(
                ticketCount,
                counts.getOrDefault(Rsvp.YES, 0L).intValue(),
                counts.getOrDefault(Rsvp.MAYBE, 0L).intValue(),
                counts.getOrDefault(Rsvp.NO, 0L).intValue(),
                counts.getOrDefault(Rsvp.PENDING, 0L).intValue(),
                members.stream().filter(m -> m.getRsvp() == Rsvp.YES).mapToInt(GroupMember::getGuestCount).sum(),
                minBudget,
                summaries,
                top,
                views.ageWarnings(group.getMovie(), members, null));
    }

    /** Suất còn cụm ghế phù hợp cho cả nhóm không (cảnh báo GRP07). */
    private OptionSummary optionSummary(GroupShowtimeOption option, int votes, int partySize, Long budget) {
        var info = GroupViews.showtimeInfo(option.getShowtime());
        SeatLayout layout;
        try {
            layout = showtimeSeatService.seatLayout(option.getShowtime().getId());
        } catch (BusinessException ex) {
            return new OptionSummary(option.getId(), info, votes, 0, "UNAVAILABLE", null);
        }
        SuggestionResult sameRow =
                planner.suggest(layout, planner.request(partySize, false, budget, Set.of()).withTopK(1));
        String cluster = "SAME_ROW";
        SuggestionResult best = sameRow;
        if (!sameRow.found()) {
            best = planner.suggest(layout, planner.request(partySize, true, budget, Set.of()).withTopK(1));
            cluster = best.found() ? "SPLIT" : "NONE";
        }
        return new OptionSummary(
                option.getId(),
                info,
                votes,
                GroupSeatPlanner.availablePlaces(layout),
                cluster,
                best.found() ? best.suggestions().get(0).score() : null);
    }

    @Override
    @Transactional
    public GroupDetailResponse lock(Long userId, Long groupId, LockGroupRequest request) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireOpen(group);
        Showtime showtime = eligibleShowtime(group, request.showtimeId());
        int ticketCount = request.ticketCount() != null
                ? request.ticketCount()
                : GroupViews.ticketCount(memberRepository.findActive(groupId));
        if (ticketCount < 1 || ticketCount > settingService.getInt(SettingKey.GROUP_MAX_SIZE)) {
            throw new BusinessException(ErrorCode.INVALID_TICKET_COUNT);
        }
        group.setStatus(GroupStatus.LOCKED);
        group.setLockedShowtimeId(showtime.getId());
        group.setLockedTicketCount(ticketCount);
        record(group, "LOCKED", userId, Map.of("showtimeId", showtime.getId(), "ticketCount", ticketCount));
        changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.LOCKED.name()));
        notifyMembers(group, userId, "GROUP_LOCKED", "Nhóm đã chốt suất chiếu",
                "Nhóm %s đã chốt suất %s (%d vé). Trưởng nhóm đang chọn ghế."
                        .formatted(group.getName(), formatTime(showtime.getStartAt()), ticketCount));
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse unlock(Long userId, Long groupId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.LOCKED);
        group.setStatus(GroupStatus.OPEN);
        group.setLockedShowtimeId(null);
        group.setLockedTicketCount(null);
        record(group, "UNLOCKED", userId, Map.of());
        changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.OPEN.name()));
        notifyMembers(group, userId, "GROUP_UNLOCKED", "Nhóm mở lại để chọn suất",
                "Trưởng nhóm đã mở chốt nhóm " + group.getName() + " để đổi suất chiếu.");
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse cancel(Long userId, Long groupId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.OPEN, GroupStatus.LOCKED, GroupStatus.HOLDING, GroupStatus.BOOKED);
        if (group.getStatus() == GroupStatus.HOLDING && group.getHoldId() != null) {
            holdService.releaseGroupHold(group.getHoldId(), userId);
        }
        if (group.getStatus() == GroupStatus.BOOKED) {
            // GRP21: hủy toàn bộ đơn nhóm theo chính sách (không hủy một phần, D9)
            orderService.cancelGroupOrder(group.getOrderId(), userId);
        }
        group.setStatus(GroupStatus.CANCELLED);
        group.setHoldId(null);
        record(group, "CANCELLED", userId, Map.of());
        changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.CANCELLED.name()));
        notifyMembers(group, userId, "GROUP_CANCELLED", "Nhóm đã bị hủy",
                "Trưởng nhóm đã hủy nhóm " + group.getName() + ".");
        return views.detail(group, userId);
    }

    // ===================================================================== Ghế, giữ ghế, đơn

    @Override
    @Transactional
    public SeatSuggestionsResponse suggestSeats(Long userId, Long groupId, SeatSuggestionRequest request) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.LOCKED, GroupStatus.HOLDING);
        int n = group.getLockedTicketCount();
        Long budget = request.maxPricePerPerson() != null ? request.maxPricePerPerson() : minBudget(groupId);
        SeatLayout layout = showtimeSeatService.seatLayout(group.getLockedShowtimeId());
        SuggestionRequest engineRequest = planner.request(n, request.allowSplit(), budget, request.seatTypes());

        long start = System.nanoTime();
        SuggestionResult result = planner.suggest(layout, engineRequest);
        int durationMs = (int) Duration.ofNanos(System.nanoTime() - start).toMillis();

        List<SuggestionView> options = rank(result);
        SeatSuggestionLog log = new SeatSuggestionLog();
        log.setGroupId(groupId);
        log.setShowtimeId(group.getLockedShowtimeId());
        log.setPartySize(n);
        log.setOptions(options.stream()
                .map(o -> Map.<String, Object>of(
                        "rank", o.rank(), "seatIds", o.seatIds(), "score", o.score(), "rows", o.rowCount()))
                .toList());
        log.setReason(result.reason() == null ? null : result.reason().name());
        log.setContiguous(!options.isEmpty() && options.get(0).rowCount() == 1);
        log.setDurationMs(durationMs);
        log.setCreatedAt(clock.instant());
        suggestionLogRepository.save(log);
        return new SeatSuggestionsResponse(
                log.getId(), n, options, result.reason() == null ? null : result.reason().name());
    }

    @Override
    @Transactional(readOnly = true)
    public SuggestionView evaluateSeats(Long userId, Long groupId, List<Long> seatIds) {
        BookingGroup group = groupForLeader(groupId, userId);
        requireStatus(group, GroupStatus.LOCKED, GroupStatus.HOLDING);
        SeatLayout layout = showtimeSeatService.seatLayout(group.getLockedShowtimeId());
        return GroupSeatPlanner.view(0, evaluateSelection(group, layout, seatIds));
    }

    @Override
    @Transactional(readOnly = true)
    public void previewSeats(Long userId, Long groupId, List<Long> seatIds) {
        BookingGroup group = groupForLeader(groupId, userId);
        requireStatus(group, GroupStatus.LOCKED);
        changed(group, "SEATS_PREVIEWED", Map.of("seatIds", List.copyOf(seatIds)));
    }

    @Override
    @Transactional
    public GroupDetailResponse hold(Long userId, Long groupId, GroupHoldRequest request) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.LOCKED);
        SeatLayout layout = showtimeSeatService.seatLayout(group.getLockedShowtimeId());
        Set<Long> seatIds = new HashSet<>(request.seatIds());
        Set<Long> unavailable = layout.seats().stream()
                .filter(s -> seatIds.contains(s.id()) && !s.available())
                .map(s -> s.id())
                .collect(Collectors.toSet());
        if (!unavailable.isEmpty()) {
            throw seatsLost(group, layout, unavailable);
        }
        evaluateSelection(group, layout, request.seatIds());

        HoldService.GroupHold hold;
        try {
            hold = holdService.holdForGroup(userId, groupId, group.getLockedShowtimeId(), request.seatIds());
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.SEAT_UNAVAILABLE) {
                throw ex;
            }
            throw seatsLost(group, layout, lostSeats(ex, seatIds));
        }
        group.setStatus(GroupStatus.HOLDING);
        group.setHoldId(hold.holdId());
        group.setOrderId(null);
        if (request.suggestionLogId() != null) {
            suggestionLogRepository
                    .findByIdAndGroupId(request.suggestionLogId(), groupId)
                    .ifPresent(log -> log.setChosenRank(request.chosenRank() == null ? 0 : request.chosenRank()));
        }
        record(group, "SEATS_HELD", userId, Map.of("seatIds", List.copyOf(seatIds), "holdId", hold.holdId()));
        changed(group, "SEATS_HELD", Map.of("seatIds", List.copyOf(seatIds)));
        notifyMembers(group, userId, "GROUP_SEATS_HELD", "Đã giữ ghế cho nhóm",
                "Trưởng nhóm đã giữ ghế cho nhóm " + group.getName() + " và đang thanh toán.");
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public GroupDetailResponse releaseHold(Long userId, Long groupId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.HOLDING);
        // Module đặt vé phát HoldClosedEvent → onHoldClosed đưa nhóm về LOCKED trong cùng transaction
        holdService.releaseGroupHold(group.getHoldId(), userId);
        return views.detail(group, userId);
    }

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, Long groupId) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.HOLDING);
        OrderResponse order = orderService.create(userId, new CreateOrderRequest(group.getHoldId()));
        if (group.getOrderId() == null) {
            group.setOrderId(orderService.findIdByCode(order.code()));
            record(group, "ORDER_CREATED", userId, Map.of("orderCode", order.code()));
            changed(group, "UPDATED", Map.of());
        }
        return order;
    }

    @Override
    @Transactional
    public GroupDetailResponse updateAssignments(Long userId, Long groupId, AssignmentRequest request) {
        BookingGroup group = lockAsLeader(groupId, userId);
        requireStatus(group, GroupStatus.BOOKED);
        Map<Long, GroupMember> members = memberRepository.findActive(groupId).stream()
                .collect(Collectors.toMap(GroupMember::userId, Function.identity()));
        Map<String, OrderService.OrderTicket> tickets = orderService.ticketsOfOrder(group.getOrderId()).stream()
                .collect(Collectors.toMap(OrderService.OrderTicket::code, Function.identity()));
        Map<Long, GroupSeatAssignment> assignments = assignmentRepository.findByGroupId(groupId).stream()
                .collect(Collectors.toMap(GroupSeatAssignment::getTicketId, Function.identity()));
        Map<Long, OrderService.TicketHolder> holders = new HashMap<>();
        for (AssignmentRequest.Item item : request.items()) {
            OrderService.OrderTicket ticket = tickets.get(item.ticketCode());
            GroupMember member = members.get(item.userId());
            if (ticket == null || member == null) {
                throw new BusinessException(ErrorCode.INVALID_ASSIGNMENT);
            }
            String guest = item.guestLabel() == null || item.guestLabel().isBlank() ? null : item.guestLabel().trim();
            GroupSeatAssignment a = assignments.computeIfAbsent(ticket.id(), id -> newAssignment(groupId, id));
            a.setUserId(member.userId());
            a.setGuestLabel(guest);
            assignmentRepository.save(a);
            holders.put(ticket.id(), holder(member, guest));
        }
        orderService.assignTicketHolders(group.getOrderId(), holders);
        record(group, "ASSIGNMENTS_UPDATED", userId, Map.of("count", holders.size()));
        changed(group, "UPDATED", Map.of());
        return views.detail(group, userId);
    }

    // ===================================================================== STOMP, sự kiện, job

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminGroupRow> adminSearch(GroupStatus status, String q, int page, int size) {
        Specification<BookingGroup> spec = (root, cq, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), status));
        }
        if (q != null && !q.isBlank()) {
            String like = "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            spec = spec.and((root, cq, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("code")), like)));
        }
        org.springframework.data.domain.Page<BookingGroup> result = groupRepository.findAll(
                spec,
                org.springframework.data.domain.PageRequest.of(
                        Math.max(page, 0),
                        Math.min(Math.max(size, 1), 100),
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));
        List<Long> ids = result.getContent().stream().map(BookingGroup::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) {
            memberRepository.countActiveByGroup(ids).forEach(row -> counts.put((Long) row[0], (Long) row[1]));
        }
        Map<Long, UserBrief> leaders = userService.briefs(
                result.getContent().stream().map(BookingGroup::getLeaderId).collect(Collectors.toSet()));
        return PageResponse.from(result.map(g -> new AdminGroupRow(
                g.getId(),
                g.getCode(),
                g.getName(),
                g.getStatus(),
                g.getMovie().getTitle(),
                leaders.get(g.getLeaderId()),
                counts.getOrDefault(g.getId(), 0L),
                g.getLockedTicketCount(),
                g.getRsvpDeadline(),
                g.getCreatedAt())));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminGroupDetail adminGet(Long groupId) {
        BookingGroup group = groupRepository
                .findWithDetailsById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        List<GroupEvent> events = eventRepository.findByGroupIdOrderByCreatedAtAscIdAsc(groupId);
        Map<Long, UserBrief> actors = userService.briefs(events.stream()
                .map(GroupEvent::getActorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return new AdminGroupDetail(
                views.detail(group, null),
                events.stream()
                        .map(e -> new AdminGroupDetail.EventView(
                                e.getType(),
                                e.getActorId(),
                                e.getActorId() == null || !actors.containsKey(e.getActorId())
                                        ? null
                                        : actors.get(e.getActorId()).fullName(),
                                e.getData(),
                                e.getCreatedAt()))
                        .toList(),
                suggestionLogRepository.findByGroupIdOrderByCreatedAtDesc(groupId).stream()
                        .map(l -> new AdminGroupDetail.SuggestionLogView(
                                l.getId(),
                                l.getShowtimeId(),
                                l.getPartySize(),
                                l.isContiguous(),
                                l.getReason(),
                                l.getChosenRank(),
                                l.getDurationMs(),
                                l.getOptions().size(),
                                l.getCreatedAt()))
                        .toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isActiveMember(Long groupId, Long userId) {
        return memberRepository.isActiveMember(groupId, userId);
    }

    @Override
    @Transactional
    public void onHoldClosed(HoldClosedEvent event) {
        groupRepository.findForUpdate(event.groupId()).ifPresent(group -> {
            if (group.getStatus() != GroupStatus.HOLDING || !Objects.equals(group.getHoldId(), event.holdId())) {
                return;
            }
            group.setStatus(GroupStatus.LOCKED);
            group.setHoldId(null);
            group.setOrderId(null);
            record(group, "HOLD_" + event.status().name(), null, Map.of("holdId", event.holdId()));
            changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.LOCKED.name()));
            notifyUsers(group, List.of(group.getLeaderId()), "GROUP_HOLD_CLOSED", "Ghế của nhóm đã được trả lại",
                    "Lượt giữ ghế của nhóm " + group.getName() + " đã kết thúc. Bạn có thể chọn lại ghế.");
        });
    }

    @Override
    @Transactional
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.groupId() == null) {
            return;
        }
        if (event.from() == OrderStatus.PAID && event.to() == OrderStatus.CANCELLED) {
            // Đơn nhóm bị hủy từ ngoài trang nhóm (admin hủy, rạp hủy suất) → nhóm cũng kết thúc
            groupRepository.findForUpdate(event.groupId())
                    .filter(g -> g.getStatus() == GroupStatus.BOOKED)
                    .ifPresent(g -> {
                        g.setStatus(GroupStatus.CANCELLED);
                        record(g, "ORDER_CANCELLED", null, Map.of("reason", String.valueOf(event.reason())));
                        changed(g, "STATUS_CHANGED", Map.of("status", GroupStatus.CANCELLED.name()));
                        if (!"GROUP_CANCEL".equals(event.reason())) {
                            notifyMembers(g, null, "GROUP_CANCELLED", "Đặt vé của nhóm đã bị hủy",
                                    "Đơn của nhóm " + g.getName() + " đã bị hủy và hoàn tiền.");
                        }
                    });
            return;
        }
        if (event.to() != OrderStatus.PAID) {
            return;
        }
        groupRepository.findForUpdate(event.groupId()).ifPresent(group -> {
            group.setStatus(GroupStatus.BOOKED);
            group.setOrderId(event.orderId());
            group.setHoldId(null);
            autoAssign(group, event.orderId());
            record(group, "PAID", group.getLeaderId(), Map.of("orderCode", event.orderCode()));
            changed(group, "PAID", Map.of());
            notifyMembers(group, null, "GROUP_PAID", "Nhóm đã đặt vé thành công",
                    "Đơn " + event.orderCode() + " của nhóm " + group.getName()
                            + " đã được thanh toán. Xem vé của bạn trong mục Vé của tôi.");
        });
    }

    @Override
    @Transactional
    public void onShowtimeCancelled(Long showtimeId) {
        for (Long id : groupRepository.findIdsByLockedShowtime(showtimeId)) {
            BookingGroup group = groupRepository.findForUpdate(id).orElse(null);
            if (group == null) {
                continue;
            }
            if (group.getStatus() == GroupStatus.LOCKED || group.getStatus() == GroupStatus.HOLDING) {
                group.setStatus(GroupStatus.OPEN);
                group.setLockedShowtimeId(null);
                group.setLockedTicketCount(null);
                group.setHoldId(null);
                group.setOrderId(null);
                record(group, "SHOWTIME_CANCELLED", null, Map.of("showtimeId", showtimeId));
                changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.OPEN.name()));
                notifyMembers(group, null, "GROUP_SHOWTIME_CANCELLED", "Suất chiếu của nhóm bị hủy",
                        "Rạp đã hủy suất chiếu nhóm " + group.getName() + " đã chốt. Nhóm quay lại bước chọn suất.");
            } else if (group.getStatus() == GroupStatus.BOOKED) {
                // Đơn của nhóm được module đặt vé hủy và hoàn 100 %
                group.setStatus(GroupStatus.CANCELLED);
                record(group, "SHOWTIME_CANCELLED", null, Map.of("showtimeId", showtimeId));
                changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.CANCELLED.name()));
                notifyMembers(group, null, "GROUP_SHOWTIME_CANCELLED", "Suất chiếu của nhóm bị hủy",
                        "Rạp đã hủy suất chiếu của nhóm " + group.getName() + ". Đơn của nhóm được hoàn tiền 100 %.");
            }
        }
    }

    @Override
    @Transactional
    public int completeFinishedGroups() {
        return groupRepository.completeFinished(clock.instant());
    }

    @Override
    @Transactional
    public int expireOverdueGroups() {
        int expired = 0;
        Instant now = clock.instant();
        for (Long id : groupRepository.findExpiredOpenIds(now)) {
            BookingGroup group = groupRepository.findForUpdate(id).orElse(null);
            if (group == null || group.getStatus() != GroupStatus.OPEN || !group.getRsvpDeadline().isBefore(now)) {
                continue;
            }
            group.setStatus(GroupStatus.EXPIRED);
            record(group, "EXPIRED", null, Map.of());
            changed(group, "STATUS_CHANGED", Map.of("status", GroupStatus.EXPIRED.name()));
            notifyMembers(group, null, "GROUP_EXPIRED", "Nhóm đã hết hạn",
                    "Nhóm " + group.getName() + " đã quá hạn chốt mà chưa chốt suất chiếu.");
            expired++;
        }
        return expired;
    }

    @Override
    @Transactional
    public int remindPendingMembers() {
        Instant now = clock.instant();
        Instant before = now.plus(Duration.ofMinutes(settingService.getInt(SettingKey.RSVP_REMINDER_BEFORE_MIN)));
        int reminded = 0;
        for (Long id : groupRepository.findReminderDueIds(now, before)) {
            BookingGroup group = groupRepository.findForUpdate(id).orElse(null);
            if (group == null || group.getReminderSentAt() != null) {
                continue;
            }
            group.setReminderSentAt(now);
            List<Long> pending = memberRepository.findActive(id).stream()
                    .filter(m -> m.getRsvp() == Rsvp.PENDING)
                    .map(GroupMember::userId)
                    .toList();
            notifyUsers(group, pending, "GROUP_RSVP_REMINDER", "Nhóm sắp tới hạn chốt",
                    "Nhóm %s sẽ chốt lúc %s — bạn chưa phản hồi có tham gia không."
                            .formatted(group.getName(), formatTime(group.getRsvpDeadline())));
            reminded++;
        }
        return reminded;
    }

    // ===================================================================== helpers

    /** Phân phối vé tự động (GRP17): trưởng nhóm → thành viên "Có" theo thứ tự tham gia → khách đi kèm. */
    private void autoAssign(BookingGroup group, Long orderId) {
        List<GroupMember> members = memberRepository.findActive(group.getId());
        List<GroupMember> attending = new ArrayList<>(members.stream().filter(m -> m.tickets() > 0).toList());
        attending.sort(Comparator.comparing((GroupMember m) -> m.getRole() != GroupRole.LEADER)
                .thenComparing(GroupMember::getJoinedAt));
        List<Object[]> people = new ArrayList<>();
        attending.forEach(m -> people.add(new Object[] {m, null}));
        for (GroupMember m : attending) {
            for (int i = 1; i <= m.getGuestCount(); i++) {
                people.add(new Object[] {m, "Khách của " + m.getUser().getFullName() + (m.getGuestCount() > 1 ? " #" + i : "")});
            }
        }
        GroupMember leader = members.stream()
                .filter(m -> m.getRole() == GroupRole.LEADER)
                .findFirst()
                .orElseThrow();
        Map<Long, GroupSeatAssignment> existing = assignmentRepository.findByGroupId(group.getId()).stream()
                .collect(Collectors.toMap(GroupSeatAssignment::getTicketId, Function.identity()));
        Map<Long, OrderService.TicketHolder> holders = new LinkedHashMap<>();
        List<OrderService.OrderTicket> tickets = orderService.ticketsOfOrder(orderId);
        for (int i = 0; i < tickets.size(); i++) {
            GroupMember member = i < people.size() ? (GroupMember) people.get(i)[0] : leader;
            String guest = i < people.size() ? (String) people.get(i)[1] : null;
            GroupSeatAssignment a = existing.computeIfAbsent(tickets.get(i).id(), id -> newAssignment(group.getId(), id));
            a.setUserId(member.userId());
            a.setGuestLabel(guest);
            assignmentRepository.save(a);
            holders.put(tickets.get(i).id(), holder(member, guest));
        }
        orderService.assignTicketHolders(orderId, holders);
    }

    private static GroupSeatAssignment newAssignment(Long groupId, Long ticketId) {
        GroupSeatAssignment a = new GroupSeatAssignment();
        a.setGroupId(groupId);
        a.setTicketId(ticketId);
        return a;
    }

    private static OrderService.TicketHolder holder(GroupMember member, String guestLabel) {
        return new OrderService.TicketHolder(
                member.userId(), guestLabel != null ? guestLabel : member.getUser().getFullName());
    }

    /** Kiểm tra số chỗ của lựa chọn: đủ cho nhóm, thừa tối đa 1 chỗ (ghế đôi). */
    private Suggestion evaluateSelection(BookingGroup group, SeatLayout layout, Collection<Long> seatIds) {
        int n = group.getLockedTicketCount();
        Suggestion evaluated;
        try {
            evaluated = planner.evaluate(layout, planner.request(n, true, minBudget(group.getId()), Set.of()), seatIds);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.SEAT_COUNT_MISMATCH);
        }
        if (evaluated.capacity() > n + 1) {
            throw new BusinessException(ErrorCode.SEAT_COUNT_MISMATCH);
        }
        return evaluated;
    }

    /** Ghế vừa bị người khác lấy → 409 kèm phương án mới tính trên sơ đồ đã loại các ghế đó (GRP14). */
    private BusinessException seatsLost(BookingGroup group, SeatLayout layout, Collection<Long> lost) {
        SeatLayout updated = GroupSeatPlanner.withUnavailable(layout, lost);
        SuggestionResult result = planner.suggest(
                updated, planner.request(group.getLockedTicketCount(), true, minBudget(group.getId()), Set.of()));
        SeatSuggestionsResponse suggestions = new SeatSuggestionsResponse(
                null,
                group.getLockedTicketCount(),
                rank(result),
                result.reason() == null ? null : result.reason().name());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("seatIds", List.copyOf(lost));
        details.put("suggestions", suggestions);
        return new BusinessException(
                ErrorCode.SEAT_UNAVAILABLE, "Một số ghế vừa được người khác giữ. Đây là các phương án mới cho nhóm.", details);
    }

    @SuppressWarnings("unchecked")
    private static Collection<Long> lostSeats(BusinessException ex, Collection<Long> requested) {
        if (ex.getDetails() instanceof Map<?, ?> details && details.get("seatIds") instanceof Collection<?> ids) {
            return (Collection<Long>) ids;
        }
        return requested;
    }

    private static List<SuggestionView> rank(SuggestionResult result) {
        List<SuggestionView> views = new ArrayList<>();
        for (int i = 0; i < result.suggestions().size(); i++) {
            views.add(GroupSeatPlanner.view(i + 1, result.suggestions().get(i)));
        }
        return views;
    }

    private Long minBudget(Long groupId) {
        return memberRepository.findActive(groupId).stream()
                .filter(m -> m.getRsvp() == Rsvp.YES && m.getMaxBudget() != null)
                .map(GroupMember::getMaxBudget)
                .min(Long::compare)
                .orElse(null);
    }

    /** Suất đề xuất/chốt phải cùng phim, cùng rạp (nếu nhóm chọn rạp), trong khoảng ngày và còn bán vé. */
    private Showtime eligibleShowtime(BookingGroup group, Long showtimeId) {
        Showtime showtime;
        try {
            showtime = showtimeSeatService.getBookable(showtimeId);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_ELIGIBLE);
        }
        LocalDate day = showtime.getStartAt().atZone(TimeZones.VIETNAM).toLocalDate();
        boolean eligible = showtime.getMovie().getId().equals(group.getMovie().getId())
                && (group.getCinema() == null
                        || showtime.getRoom().getCinema().getId().equals(group.getCinema().getId()))
                && (group.getDateFrom() == null || !day.isBefore(group.getDateFrom()))
                && (group.getDateTo() == null || !day.isAfter(group.getDateTo()));
        if (!eligible) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_ELIGIBLE);
        }
        return showtime;
    }

    private void applyEditable(
            BookingGroup group, String name, Long cinemaId, LocalDate from, LocalDate to, Instant deadline) {
        if ((from != null && to != null && from.isAfter(to)) || !deadline.isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.INVALID_GROUP_DATES);
        }
        group.setName(name.trim());
        group.setCinema(cinemaId == null ? null : cinemaService.getEntity(cinemaId));
        group.setDateFrom(from);
        group.setDateTo(to);
        group.setRsvpDeadline(deadline);
    }

    private void deactivate(BookingGroup group, GroupMember member) {
        member.setLeftAt(clock.instant());
        voteRepository.deleteByMember(group.getId(), member.userId());
    }

    private void saveVote(Long optionId, Long userId) {
        GroupVote vote = new GroupVote();
        vote.setId(new GroupVote.Key(optionId, userId));
        vote.setCreatedAt(clock.instant());
        voteRepository.save(vote);
    }

    private void checkActiveGroupLimit(Long userId) {
        if (groupRepository.countActiveForUser(userId) >= settingService.getInt(SettingKey.MAX_ACTIVE_GROUPS_PER_USER)) {
            throw new BusinessException(ErrorCode.TOO_MANY_ACTIVE_GROUPS);
        }
    }

    private BookingGroup byCode(String code) {
        return groupRepository
                .findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_CODE_INVALID));
    }

    private String newCode() {
        String code;
        do {
            char[] chars = new char[CODE_LENGTH];
            for (int i = 0; i < CODE_LENGTH; i++) {
                chars[i] = CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)];
            }
            code = new String(chars);
        } while (groupRepository.existsByCode(code));
        return code;
    }

    private BookingGroup lockGroup(Long groupId) {
        return groupRepository
                .findForUpdate(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
    }

    private BookingGroup lockAsLeader(Long groupId, Long userId) {
        BookingGroup group = lockGroup(groupId);
        requireMember(group, userId);
        requireLeader(group, userId);
        return group;
    }

    private BookingGroup groupForLeader(Long groupId, Long userId) {
        BookingGroup group = groupRepository
                .findById(groupId)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
        requireMember(group, userId);
        requireLeader(group, userId);
        return group;
    }

    /** Người không thuộc nhóm nhận GROUP_NOT_FOUND để không lộ nhóm tồn tại (IDOR). */
    private GroupMember requireMember(BookingGroup group, Long userId) {
        return memberRepository
                .findByGroupAndUser(group.getId(), userId)
                .filter(GroupMember::isActive)
                .orElseThrow(() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND));
    }

    private static void requireLeader(BookingGroup group, Long userId) {
        if (!group.getLeaderId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_GROUP_LEADER);
        }
    }

    private static void requireOpen(BookingGroup group) {
        if (group.getStatus() != GroupStatus.OPEN) {
            throw new BusinessException(ErrorCode.GROUP_NOT_OPEN);
        }
    }

    private static void requireStatus(BookingGroup group, GroupStatus... allowed) {
        for (GroupStatus status : allowed) {
            if (group.getStatus() == status) {
                return;
            }
        }
        throw new BusinessException(ErrorCode.GROUP_INVALID_STATUS);
    }

    private void record(BookingGroup group, String type, Long actorId, Map<String, Object> data) {
        GroupEvent event = new GroupEvent();
        event.setGroupId(group.getId());
        event.setType(type);
        event.setActorId(actorId);
        event.setData(data);
        event.setCreatedAt(clock.instant());
        eventRepository.save(event);
    }

    private void changed(BookingGroup group, String type, Map<String, Object> data) {
        eventPublisher.publishEvent(new GroupChangedEvent(group.getId(), type, data));
    }

    /** Thông báo mọi thành viên trừ người thực hiện. */
    private void notifyMembers(BookingGroup group, Long actorId, String type, String title, String body) {
        List<Long> recipients = memberRepository.findActive(group.getId()).stream()
                .map(GroupMember::userId)
                .filter(id -> !id.equals(actorId))
                .toList();
        notifyUsers(group, recipients, type, title, body);
    }

    private void notifyUsers(BookingGroup group, List<Long> recipients, String type, String title, String body) {
        if (!recipients.isEmpty()) {
            eventPublisher.publishEvent(new GroupNotifyEvent(group.getId(), recipients, type, title, body));
        }
    }

    private static String formatTime(Instant instant) {
        return java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy")
                .withZone(TimeZones.VIETNAM)
                .format(instant);
    }
}
