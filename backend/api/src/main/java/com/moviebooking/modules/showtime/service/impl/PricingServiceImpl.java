package com.moviebooking.modules.showtime.service.impl;

import com.moviebooking.common.util.TimeZones;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.cinema.entity.Seat;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.cinema.service.SeatTypeService;
import com.moviebooking.modules.movie.service.MovieService;
import com.moviebooking.modules.showtime.dto.request.BasePriceItem;
import com.moviebooking.modules.showtime.dto.request.HolidayRequest;
import com.moviebooking.modules.showtime.dto.response.BasePriceResponse;
import com.moviebooking.modules.showtime.dto.response.HolidayResponse;
import com.moviebooking.modules.showtime.dto.response.PriceQuoteResponse;
import com.moviebooking.modules.showtime.entity.BasePrice;
import com.moviebooking.modules.showtime.entity.Holiday;
import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.mapper.ShowtimeMapper;
import com.moviebooking.modules.showtime.repository.BasePriceRepository;
import com.moviebooking.modules.showtime.repository.HolidayRepository;
import com.moviebooking.modules.showtime.service.PricingService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingServiceImpl implements PricingService {

    private final BasePriceRepository basePriceRepository;
    private final HolidayRepository holidayRepository;
    private final SeatTypeService seatTypeService;
    private final RoomService roomService;
    private final MovieService movieService;
    private final ShowtimeMapper showtimeMapper;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public List<BasePriceResponse> listBasePrices() {
        return basePriceRepository.findAll().stream()
                .sorted(Comparator.comparing(BasePrice::getFormat).thenComparing(BasePrice::getDayType))
                .map(showtimeMapper::toBasePrice)
                .toList();
    }

    @Override
    @Transactional
    public List<BasePriceResponse> updateBasePrices(List<BasePriceItem> items) {
        Instant now = clock.instant();
        for (BasePriceItem item : items) {
            BasePrice price = basePriceRepository
                    .findById(new BasePrice.Key(item.format(), item.dayType()))
                    .orElseGet(() -> {
                        BasePrice created = new BasePrice();
                        created.setFormat(item.format());
                        created.setDayType(item.dayType());
                        return created;
                    });
            price.setPrice(item.price());
            price.setUpdatedAt(now);
            basePriceRepository.save(price);
        }
        return listBasePrices();
    }

    @Override
    @Transactional(readOnly = true)
    public List<HolidayResponse> listHolidays(LocalDate from, LocalDate to) {
        return holidayRepository.findByDateBetweenOrderByDateAsc(from, to).stream()
                .map(showtimeMapper::toHoliday)
                .toList();
    }

    @Override
    @Transactional
    public HolidayResponse addHoliday(HolidayRequest request) {
        if (holidayRepository.existsById(request.date())) {
            throw new BusinessException(ErrorCode.HOLIDAY_ALREADY_EXISTS);
        }
        Holiday holiday = new Holiday();
        holiday.setDate(request.date());
        holiday.setName(request.name().trim());
        return showtimeMapper.toHoliday(holidayRepository.save(holiday));
    }

    @Override
    @Transactional
    public void deleteHoliday(LocalDate date) {
        if (!holidayRepository.existsById(date)) {
            throw new BusinessException(ErrorCode.HOLIDAY_NOT_FOUND);
        }
        holidayRepository.deleteById(date);
    }

    @Override
    @Transactional(readOnly = true)
    public DayType dayTypeOf(Instant startAt) {
        LocalDate localDate = startAt.atZone(TimeZones.VIETNAM).toLocalDate();
        return PriceCalculator.dayType(localDate, holidayRepository.existsById(localDate));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> quote(ScreenFormat format, DayType dayType, Collection<String> seatTypeCodes) {
        long base = basePriceRepository
                .findById(new BasePrice.Key(format, dayType))
                .map(BasePrice::getPrice)
                .orElseThrow(() -> new IllegalStateException("Missing base price " + format + "/" + dayType));
        Map<String, SeatType> types = seatTypeService.getAll();
        Map<String, Long> prices = new LinkedHashMap<>();
        for (String code : seatTypeCodes) {
            SeatType type = types.get(code);
            prices.put(code, PriceCalculator.seatPrice(base, type.getCapacity(), type.getSurcharge()));
        }
        return prices;
    }

    @Override
    @Transactional(readOnly = true)
    public PriceQuoteResponse preview(Long roomId, Long movieId, ScreenFormat format, Instant startAt) {
        DayType dayType = dayTypeOf(startAt);
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        roomService.listSeats(roomId).stream().map(Seat::getSeatTypeCode).forEach(codes::add);
        Map<String, Long> prices = quote(format, dayType, codes);
        Instant endAt = movieId == null
                ? null
                : startAt.plus(Duration.ofMinutes(movieService.getEntity(movieId).getDurationMin()));
        return new PriceQuoteResponse(dayType, endAt, showtimeMapper.toPrices(prices, seatTypeService.getAll()));
    }
}
