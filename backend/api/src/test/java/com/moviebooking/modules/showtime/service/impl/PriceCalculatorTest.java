package com.moviebooking.modules.showtime.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.moviebooking.modules.showtime.enums.DayType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceCalculatorTest {

    @Test
    void classifiesDayType() {
        assertThat(PriceCalculator.dayType(LocalDate.of(2026, 10, 28), false)).isEqualTo(DayType.WEEKDAY); // thứ 4
        assertThat(PriceCalculator.dayType(LocalDate.of(2026, 10, 31), false)).isEqualTo(DayType.WEEKEND); // thứ 7
        assertThat(PriceCalculator.dayType(LocalDate.of(2026, 11, 1), false)).isEqualTo(DayType.WEEKEND); // CN
        assertThat(PriceCalculator.dayType(LocalDate.of(2026, 10, 31), true)).isEqualTo(DayType.HOLIDAY);
    }

    @Test
    void seatPriceIsBaseTimesCapacityPlusSurcharge() {
        assertThat(PriceCalculator.seatPrice(75_000, 1, 0)).isEqualTo(75_000);
        assertThat(PriceCalculator.seatPrice(75_000, 1, 20_000)).isEqualTo(95_000);
        assertThat(PriceCalculator.seatPrice(75_000, 2, 30_000)).isEqualTo(180_000);
    }
}
