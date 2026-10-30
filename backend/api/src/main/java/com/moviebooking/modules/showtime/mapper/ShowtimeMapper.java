package com.moviebooking.modules.showtime.mapper;

import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.showtime.dto.response.BasePriceResponse;
import com.moviebooking.modules.showtime.dto.response.HolidayResponse;
import com.moviebooking.modules.showtime.dto.response.SeatStats;
import com.moviebooking.modules.showtime.dto.response.ShowtimePriceResponse;
import com.moviebooking.modules.showtime.dto.response.ShowtimeResponse;
import com.moviebooking.modules.showtime.entity.BasePrice;
import com.moviebooking.modules.showtime.entity.Holiday;
import com.moviebooking.modules.showtime.entity.Showtime;
import com.moviebooking.modules.showtime.enums.ShowtimePhase;
import com.moviebooking.modules.showtime.enums.ShowtimeSeatStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.mapstruct.Mapper;

@Mapper
public interface ShowtimeMapper {

    BasePriceResponse toBasePrice(BasePrice basePrice);

    HolidayResponse toHoliday(Holiday holiday);

    default ShowtimeResponse toResponse(
            Showtime s, Map<ShowtimeSeatStatus, Long> seatCounts, Map<String, SeatType> seatTypes, Instant now) {
        return new ShowtimeResponse(
                s.getId(),
                s.getMovie().getId(),
                s.getMovie().getTitle(),
                s.getMovie().getPosterUrl(),
                s.getMovie().getAgeRating(),
                s.getMovie().getDurationMin(),
                s.getRoom().getCinema().getId(),
                s.getRoom().getCinema().getName(),
                s.getRoom().getId(),
                s.getRoom().getName(),
                s.getRoom().getRoomType(),
                s.getStartAt(),
                s.getEndAt(),
                s.getBlockedUntil(),
                s.getFormat(),
                s.getAudioType(),
                s.getDayType(),
                s.getStatus(),
                ShowtimePhase.of(s.getStatus(), s.getStartAt(), s.getEndAt(), now),
                toStats(seatCounts),
                toPrices(s.getPrices(), seatTypes));
    }

    default SeatStats toStats(Map<ShowtimeSeatStatus, Long> counts) {
        long available = counts.getOrDefault(ShowtimeSeatStatus.AVAILABLE, 0L);
        long held = counts.getOrDefault(ShowtimeSeatStatus.HELD, 0L);
        long sold = counts.getOrDefault(ShowtimeSeatStatus.SOLD, 0L);
        long blocked = counts.getOrDefault(ShowtimeSeatStatus.BLOCKED, 0L);
        return new SeatStats(available + held + sold + blocked, available, held, sold, blocked);
    }

    /** Sắp theo thứ tự loại ghế. */
    default List<ShowtimePriceResponse> toPrices(Map<String, Long> prices, Map<String, SeatType> seatTypes) {
        return prices.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> seatTypes.get(e.getKey()).getSortOrder()))
                .map(e -> new ShowtimePriceResponse(e.getKey(), seatTypes.get(e.getKey()).getName(), e.getValue()))
                .toList();
    }
}
