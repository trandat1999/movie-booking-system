package com.moviebooking.seatengine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Phòng ngẫu nhiên cho test: {@code rows × cols} ghế thường, hai lối đi dọc, lấp đầy ngẫu nhiên. */
final class RandomRooms {

    private RandomRooms() {}

    static SeatLayout room(Random random, int rows, int cols, double fillRate) {
        int aisleLeft = cols / 4;
        int aisleRight = cols - cols / 4 + 1;
        int gridCols = cols + 2;
        List<SeatSpec> seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            String label = String.valueOf((char) ('A' + r));
            int number = 0;
            for (int c = 0; c < gridCols; c++) {
                if (c == aisleLeft || c == aisleRight) {
                    continue;
                }
                boolean free = random.nextDouble() >= fillRate;
                seats.add(new SeatSpec(r * 100L + c, label, ++number, r, c, 1, 1, "STANDARD", 80_000, free));
            }
        }
        return new SeatLayout(rows, gridCols, seats);
    }
}
