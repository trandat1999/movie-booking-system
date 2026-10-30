package com.moviebooking.modules.showtime.service.impl;

import com.moviebooking.modules.showtime.enums.DayType;
import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Quy tắc giá (BKG04, SCHADM06) — lớp thuần để unit test:
 *
 * <pre>
 * giá ghế = giá cơ bản(định dạng, loại ngày) × số chỗ của ghế + phụ thu loại ghế
 * loại ngày = HOLIDAY nếu là ngày lễ, WEEKEND nếu thứ 7/CN, còn lại WEEKDAY (theo giờ Việt Nam)
 * </pre>
 */
public final class PriceCalculator {

    private PriceCalculator() {}

    public static DayType dayType(LocalDate localDate, boolean holiday) {
        if (holiday) {
            return DayType.HOLIDAY;
        }
        DayOfWeek day = localDate.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY ? DayType.WEEKEND : DayType.WEEKDAY;
    }

    public static long seatPrice(long basePrice, int capacity, long surcharge) {
        return basePrice * capacity + surcharge;
    }
}
