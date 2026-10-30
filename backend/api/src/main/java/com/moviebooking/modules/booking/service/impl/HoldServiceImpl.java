package com.moviebooking.modules.booking.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.booking.dto.request.HoldRequest;
import com.moviebooking.modules.booking.dto.response.HoldResponse;
import com.moviebooking.modules.booking.dto.response.SeatLine;
import com.moviebooking.modules.booking.entity.SeatHold;
import com.moviebooking.modules.booking.enums.HoldStatus;
import com.moviebooking.modules.booking.enums.OrderStatus;
import com.moviebooking.modules.booking.repository.SeatHoldRepository;
import com.moviebooking.modules.booking.service.HoldService;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HoldServiceImpl implements HoldService {

    private final SeatHoldRepository seatHoldRepository;
    private final ShowtimeSeatService showtimeSeatService;
    private final RoomService roomService;
    private final SettingService settingService;
    private final BookingTransitions transitions;
    private final Clock clock;

    @Override
    @Transactional
    public HoldResponse hold(Long userId, HoldRequest request) {
        Set<Long> seatIds = new LinkedHashSet<>(request.seatIds());
        if (seatIds.size() > settingService.getInt(SettingKey.MAX_SEATS_PER_BOOKING)) {
            throw new BusinessException(ErrorCode.TOO_MANY_SEATS);
        }
        Showtime showtime = showtimeSeatService.getBookable(request.showtimeId());

        // Chọn lại ghế cho cùng suất → thay lượt giữ cũ (ghế cũ được trả trước khi kiểm tra ghế mới)
        seatHoldRepository
                .findActiveIds(userId, showtime.getId())
                .forEach(previousId -> transitions.closeHold(
                        previousId,
                        userId,
                        null,
                        HoldStatus.RELEASED,
                        OrderStatus.CANCELLED,
                        "REPLACED_BY_NEW_HOLD",
                        userId));

        Instant now = clock.instant();
        if (seatHoldRepository.countActive(userId, now)
                >= settingService.getInt(SettingKey.MAX_ACTIVE_HOLDS_PER_USER)) {
            throw new BusinessException(ErrorCode.TOO_MANY_ACTIVE_HOLDS);
        }

        Map<Long, Seat> roomSeats = roomService.listSeats(showtime.getRoom().getId()).stream()
                .collect(Collectors.toMap(Seat::getId, Function.identity()));
        Map<Long, ShowtimeSeatStatus> states = showtimeSeatService.getStates(showtime.getId());
        boolean allFree = seatIds.stream()
                .allMatch(id -> roomSeats.containsKey(id) && states.get(id) == ShowtimeSeatStatus.AVAILABLE);
        if (!allFree) {
            throw new BusinessException(ErrorCode.SEAT_UNAVAILABLE);
        }

        List<String> warnings = new ArrayList<>();
        List<String> orphans = OrphanSeatChecker.findOrphans(
                roomSeats.values().stream()
                        .map(seat -> new OrphanSeatChecker.RowSeat(
                                seat.getId(),
                                seat.label(),
                                seat.getGridRow(),
                                seat.getGridCol(),
                                seat.getColSpan(),
                                states.get(seat.getId()) == ShowtimeSeatStatus.AVAILABLE))
                        .toList(),
                seatIds);
        if (!orphans.isEmpty()) {
            String message = "Lựa chọn này để trống ghế lẻ " + String.join(", ", orphans);
            switch (settingService.getString(SettingKey.ORPHAN_SEAT_RULE)) {
                case "block" -> throw new BusinessException(
                        ErrorCode.ORPHAN_SEAT, message + ". Vui lòng chọn ghế liền nhau.", Map.of("seats", orphans));
                case "warn" -> warnings.add(message);
                default -> {
                    // off
                }
            }
        }

        SeatHold hold = new SeatHold();
        hold.setUserId(userId);
        hold.setShowtimeId(showtime.getId());
        hold.setExpiresAt(now.plusSeconds(settingService.getInt(SettingKey.HOLD_TTL_SINGLE_SEC)));
        seatHoldRepository.saveAndFlush(hold);

        List<Long> held = showtimeSeatService.hold(showtime.getId(), seatIds, hold.getId(), hold.getExpiresAt());
        if (held.size() != seatIds.size()) {
            // Có người vừa giữ trước — rollback toàn bộ, không giữ một phần (BR03)
            throw new BusinessException(ErrorCode.SEAT_UNAVAILABLE);
        }

        List<SeatLine> lines = seatIds.stream()
                .map(roomSeats::get)
                .sorted(Comparator.comparingInt(Seat::getGridRow).thenComparingInt(Seat::getGridCol))
                .map(seat -> new SeatLine(
                        seat.getId(),
                        seat.label(),
                        seat.getSeatTypeCode(),
                        showtime.getPrices().getOrDefault(seat.getSeatTypeCode(), 0L)))
                .toList();
        long total = lines.stream().mapToLong(SeatLine::price).sum();
        return new HoldResponse(
                hold.getId(), showtime.getId(), hold.getExpiresAt(), lines, total, warnings, clock.instant());
    }

    @Override
    @Transactional
    public void release(Long userId, Long holdId) {
        transitions.closeHold(
                holdId, userId, null, HoldStatus.RELEASED, OrderStatus.CANCELLED, "HOLD_RELEASED", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findDueIds(int limit) {
        return seatHoldRepository.findDueIds(clock.instant(), PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public boolean expire(Long holdId) {
        return transitions.closeHold(
                holdId, null, clock.instant(), HoldStatus.EXPIRED, OrderStatus.EXPIRED, "EXPIRED", null);
    }
}
