package com.moviebooking.seatengine;

import java.util.Objects;

/**
 * Một ghế trong lưới phòng chiếu kèm trạng thái của suất đang xét.
 *
 * <p>{@code (gridRow, gridCol)} là ô trái nhất ghế chiếm; ghế đôi chiếm {@code colSpan = 2} ô và
 * {@code capacity = 2} chỗ. Hàng 0 là hàng gần màn hình nhất. Hai ghế cùng hàng liền kề khi
 * {@code colB = colA + colSpanA}; ô trống ở giữa là lối đi.
 *
 * @param price giá của cả ghế (ghế đôi: giá cho 2 người), VND
 * @param available ghế còn trống trong suất chiếu (chưa giữ/bán/khóa)
 */
public record SeatSpec(
        long id,
        String rowLabel,
        int number,
        int gridRow,
        int gridCol,
        int colSpan,
        int capacity,
        String typeCode,
        long price,
        boolean available) {

    public SeatSpec {
        Objects.requireNonNull(rowLabel, "rowLabel");
        Objects.requireNonNull(typeCode, "typeCode");
        if (gridRow < 0 || gridCol < 0 || colSpan < 1 || capacity < 1 || price < 0) {
            throw new IllegalArgumentException("Invalid seat " + rowLabel + number);
        }
    }

    public String label() {
        return rowLabel + number;
    }

    /** Cột ngay sau ghế (không thuộc ghế). */
    int colEnd() {
        return gridCol + colSpan;
    }

    double centerX() {
        return gridCol + colSpan / 2.0;
    }
}
