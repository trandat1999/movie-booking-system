package com.moviebooking.modules.showtime.service.impl;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.enums.CinemaStatus;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.cinema.service.SeatTypeService;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import com.moviebooking.modules.showtime.event.SeatStatesChangedEvent;
import com.moviebooking.modules.showtime.repository.ShowtimeRepository;
import com.moviebooking.modules.showtime.repository.ShowtimeSeatRepository;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowtimeSeatServiceImpl implements ShowtimeSeatService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final SettingService settingService;
    private final RoomService roomService;
    private final SeatTypeService seatTypeService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public Showtime getBookable(Long showtimeId) {
        Showtime showtime = showtimeRepository
                .findWithDetailsById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND));
        Instant saleClosedBefore = clock.instant()
                .minus(Duration.ofMinutes(settingService.getInt(SettingKey.SALE_CLOSE_AFTER_START_MIN)));
        boolean bookable = showtime.getStatus() == ShowtimeStatus.SCHEDULED
                && showtime.getRoom().isActive()
                && showtime.getRoom().getCinema().getStatus() == CinemaStatus.ACTIVE
                && showtime.getStartAt().isAfter(saleClosedBefore);
        if (!bookable) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND);
        }
        return showtime;
    }

    @Override
    @Transactional(readOnly = true)
    public Showtime getShowtime(Long showtimeId) {
        return showtimeRepository
                .findWithDetailsById(showtimeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatLayout seatLayout(Long showtimeId) {
        Showtime showtime = getBookable(showtimeId);
        Map<Long, ShowtimeSeatStatus> states = getStates(showtimeId);
        Map<String, SeatType> types = seatTypeService.getAll();
        List<SeatSpec> seats = roomService.listSeats(showtime.getRoom().getId()).stream()
                .map(seat -> new SeatSpec(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getNumber(),
                        seat.getGridRow(),
                        seat.getGridCol(),
                        seat.getColSpan(),
                        types.containsKey(seat.getSeatTypeCode()) ? types.get(seat.getSeatTypeCode()).getCapacity() : 1,
                        seat.getSeatTypeCode(),
                        showtime.getPrices().getOrDefault(seat.getSeatTypeCode(), 0L),
                        states.get(seat.getId()) == ShowtimeSeatStatus.AVAILABLE))
                .toList();
        return new SeatLayout(showtime.getRoom().getGridRows(), showtime.getRoom().getGridCols(), seats);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, ShowtimeSeatStatus> getStates(Long showtimeId) {
        return showtimeSeatRepository.findStates(showtimeId, clock.instant());
    }

    @Override
    @Transactional
    public List<Long> hold(Long showtimeId, Collection<Long> seatIds, Long holdId, Instant expiresAt) {
        List<Long> held = showtimeSeatRepository.hold(showtimeId, seatIds, holdId, expiresAt, clock.instant());
        // Chỉ phát sự kiện khi giữ đủ: nếu thiếu, nơi gọi ném lỗi và giao dịch rollback (sự kiện AFTER_COMMIT bị bỏ)
        if (held.size() == seatIds.size()) {
            publish(showtimeId, held, ShowtimeSeatStatus.HELD);
        }
        return held;
    }

    @Override
    @Transactional
    public List<Long> release(Long showtimeId, Long holdId) {
        List<Long> released = showtimeSeatRepository.release(holdId);
        publish(showtimeId, released, ShowtimeSeatStatus.AVAILABLE);
        return released;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> seatIdsOfHold(Long holdId) {
        return showtimeSeatRepository.seatIdsOfHold(holdId);
    }

    @Override
    @Transactional
    public boolean claim(Long showtimeId, Collection<Long> seatIds, Long holdId, Long orderId) {
        Instant now = clock.instant();
        List<ShowtimeSeatRepository.LockedSeat> locked = showtimeSeatRepository.lockSeats(showtimeId, seatIds);
        boolean claimable = locked.size() == seatIds.size()
                && locked.stream().allMatch(seat -> switch (seat.status()) {
                    case AVAILABLE -> true;
                    case HELD -> Objects.equals(seat.holdId(), holdId) || seat.heldUntil().isBefore(now);
                    case SOLD, BLOCKED -> false;
                });
        if (!claimable) {
            return false;
        }
        showtimeSeatRepository.markSold(showtimeId, seatIds, holdId, orderId);
        publish(showtimeId, List.copyOf(seatIds), ShowtimeSeatStatus.SOLD);
        return true;
    }

    private void publish(Long showtimeId, List<Long> seatIds, ShowtimeSeatStatus status) {
        if (seatIds.isEmpty()) {
            return;
        }
        Map<Long, ShowtimeSeatStatus> changes = new LinkedHashMap<>();
        seatIds.forEach(id -> changes.put(id, status));
        eventPublisher.publishEvent(new SeatStatesChangedEvent(showtimeId, changes));
    }
}
