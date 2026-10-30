package com.moviebooking.seatengine;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Sơ đồ phòng chiếu: lưới {@code gridRows × gridCols} và các ghế của suất đang xét. */
public record SeatLayout(int gridRows, int gridCols, List<SeatSpec> seats) {

    public SeatLayout {
        if (gridRows < 1 || gridCols < 1) {
            throw new IllegalArgumentException("Grid must have at least one row and column");
        }
        seats = List.copyOf(seats);
        Set<Long> ids = new HashSet<>();
        for (SeatSpec seat : seats) {
            if (seat.gridRow() >= gridRows || seat.colEnd() > gridCols) {
                throw new IllegalArgumentException("Seat " + seat.label() + " is outside the grid");
            }
            if (!ids.add(seat.id())) {
                throw new IllegalArgumentException("Duplicate seat id " + seat.id());
            }
        }
    }
}
