package com.moviebooking.modules.showtime.service.impl;

import com.moviebooking.common.util.TimeZones;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.cinema.service.SeatTypeService;
import com.moviebooking.modules.setting.enums.SettingKey;
import com.moviebooking.modules.setting.service.SettingService;
import com.moviebooking.modules.showtime.dto.response.PublicShowtimeDetailResponse;
import com.moviebooking.modules.showtime.dto.response.PublicShowtimeResponse;
import com.moviebooking.modules.showtime.dto.response.SeatMapResponse;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import com.moviebooking.modules.showtime.enums.ShowtimeStatus;
import com.moviebooking.modules.showtime.mapper.ShowtimeMapper;
import com.moviebooking.modules.showtime.repository.ShowtimeRepository;
import com.moviebooking.modules.showtime.repository.ShowtimeSeatRepository;
import com.moviebooking.modules.showtime.repository.ShowtimeSpecifications;
import com.moviebooking.modules.showtime.service.PublicShowtimeService;
import com.moviebooking.modules.showtime.service.ShowtimeSeatService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PublicShowtimeServiceImpl implements PublicShowtimeService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowtimeSeatRepository showtimeSeatRepository;
    private final RoomService roomService;
    private final SeatTypeService seatTypeService;
    private final SettingService settingService;
    private final ShowtimeSeatService showtimeSeatService;
    private final ShowtimeMapper showtimeMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<PublicShowtimeResponse> search(Long movieId, Long cinemaId, LocalDate date, ScreenFormat format) {
        Instant now = clock.instant();
        LocalDate day = date != null ? date : LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        Specification<Showtime> spec = Specification.allOf(
                ShowtimeSpecifications.hasStatus(ShowtimeStatus.SCHEDULED),
                ShowtimeSpecifications.inActiveVenue(),
                ShowtimeSpecifications.startsAfter(saleClosedBefore(now)),
                ShowtimeSpecifications.startsBetween(
                        day.atStartOfDay(TimeZones.VIETNAM).toInstant(),
                        day.plusDays(1).atStartOfDay(TimeZones.VIETNAM).toInstant()),
                ShowtimeSpecifications.ofMovie(movieId),
                ShowtimeSpecifications.inCinema(cinemaId),
                ShowtimeSpecifications.hasFormat(format));
        List<Showtime> showtimes = showtimeRepository.findAll(spec, Sort.by("startAt").and(Sort.by("id")));
        Map<Long, Map<ShowtimeSeatStatus, Long>> counts =
                showtimeSeatRepository.countByStatus(showtimes.stream().map(Showtime::getId).toList(), now);
        return showtimes.stream()
                .map(s -> toResponse(s, counts.getOrDefault(s.getId(), Map.of())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PublicShowtimeDetailResponse get(Long showtimeId) {
        Showtime showtime = findBookable(showtimeId);
        Map<ShowtimeSeatStatus, Long> counts = showtimeSeatRepository
                .countByStatus(List.of(showtimeId), clock.instant())
                .getOrDefault(showtimeId, Map.of());
        return new PublicShowtimeDetailResponse(
                toResponse(showtime, counts),
                showtime.getRoom().getCinema().getAddress() + ", " + showtime.getRoom().getCinema().getCity(),
                showtimeMapper.toPrices(showtime.getPrices(), seatTypeService.getAll()));
    }

    @Override
    @Transactional(readOnly = true)
    public SeatMapResponse getSeatMap(Long showtimeId) {
        Showtime showtime = findBookable(showtimeId);
        Map<Long, ShowtimeSeatStatus> states = showtimeSeatRepository.findStates(showtimeId, clock.instant());
        List<Seat> seats = roomService.listSeats(showtime.getRoom().getId());
        Map<String, SeatType> types = seatTypeService.getAll();

        List<SeatMapResponse.SeatMapSeat> seatViews = seats.stream()
                .map(seat -> new SeatMapResponse.SeatMapSeat(
                        seat.getId(),
                        seat.getRowLabel(),
                        seat.getNumber(),
                        seat.getGridRow(),
                        seat.getGridCol(),
                        seat.getColSpan(),
                        seat.getSeatTypeCode(),
                        states.getOrDefault(seat.getId(), ShowtimeSeatStatus.BLOCKED)))
                .toList();
        List<SeatMapResponse.SeatTypePrice> typePrices = showtime.getPrices().entrySet().stream()
                .map(e -> {
                    SeatType type = types.get(e.getKey());
                    return new SeatMapResponse.SeatTypePrice(
                            type.getCode(), type.getName(), type.getCapacity(), type.getColor(), e.getValue());
                })
                .sorted(Comparator.comparingInt(p -> types.get(p.code()).getSortOrder()))
                .toList();
        return new SeatMapResponse(
                showtimeId,
                showtime.getRoom().getGridRows(),
                showtime.getRoom().getGridCols(),
                seatViews,
                typePrices);
    }

    private Showtime findBookable(Long showtimeId) {
        return showtimeSeatService.getBookable(showtimeId);
    }

    /** Suất bắt đầu trước mốc này đã ngừng bán vé (BR16). */
    private Instant saleClosedBefore(Instant now) {
        return now.minus(Duration.ofMinutes(settingService.getInt(SettingKey.SALE_CLOSE_AFTER_START_MIN)));
    }

    private static PublicShowtimeResponse toResponse(Showtime s, Map<ShowtimeSeatStatus, Long> counts) {
        return new PublicShowtimeResponse(
                s.getId(),
                s.getMovie().getId(),
                s.getMovie().getTitle(),
                s.getMovie().getSlug(),
                s.getMovie().getPosterUrl(),
                s.getMovie().getAgeRating(),
                s.getMovie().getDurationMin(),
                s.getRoom().getCinema().getId(),
                s.getRoom().getCinema().getName(),
                s.getRoom().getCinema().getCity(),
                s.getRoom().getId(),
                s.getRoom().getName(),
                s.getRoom().getRoomType(),
                s.getStartAt(),
                s.getEndAt(),
                s.getFormat(),
                s.getAudioType(),
                counts.getOrDefault(ShowtimeSeatStatus.AVAILABLE, 0L),
                s.getPrices().values().stream().mapToLong(Long::longValue).min().orElse(0));
    }
}
