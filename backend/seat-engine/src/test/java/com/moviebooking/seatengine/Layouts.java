package com.moviebooking.seatengine;

import java.util.ArrayList;
import java.util.List;

/**
 * Dựng sơ đồ từ chuỗi cho test, mỗi chuỗi là một hàng (hàng đầu gần màn hình nhất):
 *
 * <ul>
 *   <li>{@code S} ghế thường trống, {@code X} ghế thường đã bán
 *   <li>{@code V} ghế VIP trống, {@code W} ghế VIP đã bán
 *   <li>{@code C=} ghế đôi trống (chiếm 2 ô), {@code K=} ghế đôi đã bán
 *   <li>{@code .} lối đi / ô trống
 * </ul>
 *
 * Id ghế = hàng · 100 + cột, nhãn = chữ cái hàng + số thứ tự ghế trong hàng.
 */
final class Layouts {

    static final long STANDARD_PRICE = 80_000;
    static final long VIP_PRICE = 100_000;
    static final long COUPLE_PRICE = 180_000;

    private Layouts() {}

    static SeatLayout parse(String... rows) {
        int cols = 0;
        List<SeatSpec> seats = new ArrayList<>();
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            cols = Math.max(cols, row.length());
            String label = String.valueOf((char) ('A' + r));
            int number = 0;
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                switch (ch) {
                    case 'S', 'X' -> seats.add(seat(r, c, label, ++number, 1, 1, "STANDARD", STANDARD_PRICE, ch == 'S'));
                    case 'V', 'W' -> seats.add(seat(r, c, label, ++number, 1, 1, "VIP", VIP_PRICE, ch == 'V'));
                    case 'C', 'K' -> {
                        if (c + 1 >= row.length() || row.charAt(c + 1) != '=') {
                            throw new IllegalArgumentException("Couple seat must be followed by '=': " + row);
                        }
                        seats.add(seat(r, c, label, ++number, 2, 2, "COUPLE", COUPLE_PRICE, ch == 'C'));
                    }
                    case '.', '=' -> {
                        // lối đi hoặc ô thứ hai của ghế đôi
                    }
                    default -> throw new IllegalArgumentException("Unknown cell '" + ch + "' in " + row);
                }
            }
        }
        return new SeatLayout(rows.length, cols, seats);
    }

    static long id(int row, int col) {
        return row * 100L + col;
    }

    private static SeatSpec seat(
            int row, int col, String label, int number, int span, int capacity, String type, long price, boolean free) {
        return new SeatSpec(id(row, col), label, number, row, col, span, capacity, type, price, free);
    }
}
