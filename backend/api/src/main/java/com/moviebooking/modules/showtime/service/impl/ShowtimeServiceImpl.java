package com.moviebooking.modules.showtime.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.common.util.TimeZones;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.cinema.entity.Room;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.enums.CinemaStatus;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.cinema.service.SeatTypeService;
import com.moviebooking.modules.movie.entity.Movie;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.service.MovieService;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.modules.showtime.dto.request.ShowtimeRequest;
import com.moviebooking.modules.showtime.dto.request.ShowtimeUpdateRequest;
import com.moviebooking.modules.showtime.dto.response.ConflictingShowtime;
import com.moviebooking.modules.showtime.dto.response.ShowtimeResponse;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import com.moviebooking.modules.showtime.mapper.ShowtimeMapper;
import com.moviebooking.modules.showtime.repository.ShowtimeRepository;
import com.moviebooking.modules.showtime.repository.ShowtimeSeatRepository;
import com.moviebooking.modules.showtime.repository.ShowtimeSpecifications;
import com.moviebooking.modules.showtime.service.PricingService;
import com.moviebooking.modules.showtime.service.ShowtimeService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import com.moviebooking.modules.showtime.event.ShowtimeCancelledEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final int MAX_PAGE_SIZE = 200;

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final MovieService movieService;
    private final RoomService roomService;
    private final SeatTypeService seatTypeService;
    private final PricingService pricingService;
    private final SettingService settingService;
    private final ShowtimeMapper showtimeMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> search(
            Long cinemaId, Long roomId, Long movieId, LocalDate date, ShowtimeStatus status, int page, int size) {
        Instant from = date == null ? null : date.atStartOfDay(TimeZones.VIETNAM).toInstant();
        Instant to = date == null ? null : date.plusDays(1).atStartOfDay(TimeZones.VIETNAM).toInstant();
        Specification<Showtime> spec = Specification.allOf(
                ShowtimeSpecifications.inCinema(cinemaId),
                ShowtimeSpecifications.inRoom(roomId),
                ShowtimeSpecifications.ofMovie(movieId),
                ShowtimeSpecifications.hasStatus(status),
                ShowtimeSpecifications.startsBetween(from, to));
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("startAt").and(Sort.by("id")));
        Page<Showtime> result = showtimeRepository.findAll(spec, pageable);

        Instant now = clock.instant();
        Map<Long, Map<ShowtimeSeatStatus, Long>> counts = showtimeSeatRepository.countByStatus(
                result.getContent().stream().map(Showtime::getId).toList(), now);
        Map<String, SeatType> seatTypes = seatTypeService.getAll();
        return PageResponse.from(result.map(s ->
                showtimeMapper.toResponse(s, counts.getOrDefault(s.getId(), Map.of()), seatTypes, now)));
    }

    @Override
    @Transactional(readOnly = true)
    public ShowtimeResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        Instant now = clock.instant();
        Movie movie = movieService.getEntity(request.movieId());
        if (movie.getStatus() == MovieStatus.ENDED) {
            throw new BusinessException(ErrorCode.MOVIE_NOT_SCHEDULABLE);
        }
        Room room = roomService.getEntity(request.roomId());
        if (!room.isActive() || room.getCinema().getStatus() != CinemaStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ROOM_NOT_SCHEDULABLE);
        }
        List<Seat> seats = roomService.listSeats(room.getId());
        if (seats.isEmpty()) {
            throw new BusinessException(ErrorCode.ROOM_NOT_SCHEDULABLE);
        }
        if (!request.startAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.SHOWTIME_IN_PAST);
        }
        // Khóa dòng phòng: các yêu cầu tạo suất cùng phòng chạy tuần tự, và sơ đồ ghế không đổi được nữa
        roomService.lockLayout(room.getId());

        Showtime showtime = new Showtime();
        showtime.setMovie(movie);
        showtime.setRoom(room);
        applySchedule(showtime, request.startAt(), request.format(), request.audioType(), request.prices(), seats);
        try {
            showtimeRepository.saveAndFlush(showtime);
        } catch (DataIntegrityViolationException ex) {
            // Chốt chặn cuối: ràng buộc EXCLUDE trong CSDL
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAP);
        }
        showtimeSeatRepository.insertAll(
                showtime.getId(),
                seats.stream()
                        .map(seat -> new ShowtimeSeatRepository.SeatState(
                                seat.getId(), seat.isActive() ? ShowtimeSeatStatus.AVAILABLE : ShowtimeSeatStatus.BLOCKED))
                        .toList());
        return toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse update(Long id, ShowtimeUpdateRequest request) {
        Showtime showtime = find(id);
        ensureEditable(showtime);
        if (!request.startAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.SHOWTIME_IN_PAST);
        }
        roomService.lockLayout(showtime.getRoom().getId());
        applySchedule(
                showtime,
                request.startAt(),
                request.format(),
                request.audioType(),
                request.prices(),
                roomService.listSeats(showtime.getRoom().getId()));
        try {
            showtimeRepository.saveAndFlush(showtime);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException(ErrorCode.SHOWTIME_OVERLAP);
        }
        return toResponse(showtime);
    }

    @Override
    @Transactional
    public ShowtimeResponse cancel(Long id, Long actorId) {
        Showtime showtime = find(id);
        // Được hủy cả khi đã bán vé (SCHADM03): module đặt vé hủy đơn và hoàn tiền 100 % qua sự kiện
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED || !showtime.getStartAt().isAfter(clock.instant())) {
            throw new BusinessException(ErrorCode.SHOWTIME_ALREADY_STARTED);
        }
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        eventPublisher.publishEvent(new ShowtimeCancelledEvent(showtime.getId(), actorId));
        return toResponse(showtime);
    }

    /** Tính giờ kết thúc, kiểm tra trùng lịch, loại ngày và giá. */
    private void applySchedule(
            Showtime showtime,
            Instant startAt,
            ScreenFormat format,
            AudioType audioType,
            Map<String, Long> priceOverrides,
            List<Seat> seats) {
        Instant endAt = startAt.plus(Duration.ofMinutes(showtime.getMovie().getDurationMin()));
        Instant blockedUntil = endAt.plus(Duration.ofMinutes(settingService.getInt(SettingKey.CLEANING_BUFFER_MIN)));

        List<Showtime> conflicts =
                showtimeRepository.findOverlapping(showtime.getRoom().getId(), startAt, blockedUntil, showtime.getId());
        if (!conflicts.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.SHOWTIME_OVERLAP,
                    ErrorCode.SHOWTIME_OVERLAP.getDefaultMessage(),
                    conflicts.stream()
                            .map(c -> new ConflictingShowtime(
                                    c.getId(), c.getMovie().getTitle(), c.getStartAt(), c.getEndAt(), c.getBlockedUntil()))
                            .toList());
        }

        Set<String> seatTypeCodes = new LinkedHashSet<>();
        seats.forEach(seat -> seatTypeCodes.add(seat.getSeatTypeCode()));
        var dayType = pricingService.dayTypeOf(startAt);
        Map<String, Long> prices = pricingService.quote(format, dayType, seatTypeCodes);
        if (priceOverrides != null) {
            for (Map.Entry<String, Long> override : priceOverrides.entrySet()) {
                if (!prices.containsKey(override.getKey())) {
                    throw new BusinessException(ErrorCode.SEAT_TYPE_NOT_FOUND,
                            "Phòng không có loại ghế " + override.getKey());
                }
                prices.put(override.getKey(), override.getValue());
            }
        }

        showtime.setStartAt(startAt);
        showtime.setEndAt(endAt);
        showtime.setBlockedUntil(blockedUntil);
        showtime.setFormat(format);
        showtime.setAudioType(audioType);
        showtime.setDayType(dayType);
        showtime.getPrices().clear();
        showtime.getPrices().putAll(prices);
    }

    private void ensureEditable(Showtime showtime) {
        Instant now = clock.instant();
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED
                || !showtime.getStartAt().isAfter(now)
                || showtimeSeatRepository.hasHeldOrSoldSeats(showtime.getId(), now)) {
            throw new BusinessException(ErrorCode.SHOWTIME_NOT_EDITABLE);
        }
    }

    private ShowtimeResponse toResponse(Showtime showtime) {
        Instant now = clock.instant();
        Map<ShowtimeSeatStatus, Long> counts =
                showtimeSeatRepository.countByStatus(List.of(showtime.getId()), now).getOrDefault(showtime.getId(), Map.of());
        return showtimeMapper.toResponse(showtime, counts, seatTypeService.getAll(), now);
    }

    private Showtime find(Long id) {
        return showtimeRepository
                .findWithDetailsById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOWTIME_NOT_FOUND));
    }
}
