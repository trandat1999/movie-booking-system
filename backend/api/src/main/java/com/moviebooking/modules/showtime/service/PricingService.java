package com.moviebooking.modules.showtime.service;

import com.moviebooking.modules.showtime.dto.request.BasePriceItem;
import com.moviebooking.modules.showtime.dto.request.HolidayRequest;
import com.moviebooking.modules.showtime.dto.response.BasePriceResponse;
import com.moviebooking.modules.showtime.dto.response.HolidayResponse;
import com.moviebooking.modules.showtime.dto.response.PriceQuoteResponse;
import com.moviebooking.modules.showtime.enums.DayType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Bảng giá cơ bản, ngày lễ và tính giá cho suất chiếu (SCHADM06). */
public interface PricingService {

    List<BasePriceResponse> listBasePrices();

    List<BasePriceResponse> updateBasePrices(List<BasePriceItem> items);

    List<HolidayResponse> listHolidays(LocalDate from, LocalDate to);

    HolidayResponse addHoliday(HolidayRequest request);

    void deleteHoliday(LocalDate date);

    /** Loại ngày của giờ chiếu (theo giờ Việt Nam). */
    DayType dayTypeOf(Instant startAt);

    /** Giá từng loại ghế theo bảng giá hiện tại. */
    Map<String, Long> quote(ScreenFormat format, DayType dayType, Collection<String> seatTypeCodes);

    /** Xem trước giá khi tạo suất chiếu ở phòng {@code roomId}. */
    PriceQuoteResponse preview(Long roomId, Long movieId, ScreenFormat format, Instant startAt);
}
