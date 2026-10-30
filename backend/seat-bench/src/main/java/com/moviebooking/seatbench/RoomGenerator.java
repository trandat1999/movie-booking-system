package com.moviebooking.seatbench;

import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Sinh phòng chiếu và trạng thái ghế cho thực nghiệm (PLAN.md 8.2): 100 / 200 / 300 ghế, hai lối đi dọc chia
 * phòng thành 3 khối, khối VIP ở giữa phía sau; tỷ lệ lấp đầy chính xác theo một trong hai kiểu.
 */
public final class RoomGenerator {

    public enum RoomSize {
        S100(10, 10),
        S200(10, 20),
        S300(15, 20);

        final int rows;
        final int seatsPerRow;

        RoomSize(int rows, int seatsPerRow) {
            this.rows = rows;
            this.seatsPerRow = seatsPerRow;
        }

        public int seats() {
            return rows * seatsPerRow;
        }
    }

    public enum FillPattern {
        /** Ghế đã bán phân bố đều ngẫu nhiên. */
        RANDOM,
        /** Ghế gần "điểm xem đẹp" bán trước (khán giả thường chọn giữa phòng). */
        CENTER
    }

    private RoomGenerator() {}

    public static SeatLayout generate(RoomSize size, double fillRate, FillPattern pattern, Random random) {
        int aisleLeft = size.seatsPerRow / 4;
        int aisleRight = size.seatsPerRow - size.seatsPerRow / 4 + 1;
        int gridCols = size.seatsPerRow + 2;
        List<SeatSpec> template = new ArrayList<>(size.seats());
        for (int r = 0; r < size.rows; r++) {
            String label = String.valueOf((char) ('A' + r));
            int number = 0;
            for (int c = 0; c < gridCols; c++) {
                if (c == aisleLeft || c == aisleRight) {
                    continue;
                }
                boolean vip = r >= size.rows / 2 && c > aisleLeft && c < aisleRight;
                template.add(new SeatSpec(
                        r * 100L + c, label, ++number, r, c, 1, 1, vip ? "VIP" : "STANDARD", vip ? 100_000 : 80_000,
                        true));
            }
        }

        int sold = (int) Math.round(fillRate * template.size());
        Set<Long> soldIds = new HashSet<>();
        List<SeatSpec> order = new ArrayList<>(template);
        if (pattern == FillPattern.RANDOM) {
            Collections.shuffle(order, random);
        } else {
            // Lấy mẫu có trọng số không hoàn lại (Efraimidis–Spirakis): khóa = u^(1/w), w lớn gần trung tâm
            double sweetX = gridCols / 2.0;
            double sweetY = size.rows * 2.0 / 3.0;
            List<double[]> keyed = new ArrayList<>();
            for (int i = 0; i < order.size(); i++) {
                SeatSpec s = order.get(i);
                double dx = (s.gridCol() + 0.5 - sweetX) / sweetX;
                double dy = (s.gridRow() + 0.5 - sweetY) / size.rows;
                double weight = Math.exp(-(dx * dx + dy * dy) / 0.15);
                keyed.add(new double[] {Math.pow(random.nextDouble(), 1 / weight), i});
            }
            keyed.sort(Comparator.comparingDouble((double[] k) -> k[0]).reversed());
            List<SeatSpec> weighted = new ArrayList<>(order.size());
            keyed.forEach(k -> weighted.add(order.get((int) k[1])));
            order.clear();
            order.addAll(weighted);
        }
        for (int i = 0; i < sold; i++) {
            soldIds.add(order.get(i).id());
        }

        List<SeatSpec> seats = template.stream()
                .map(s -> soldIds.contains(s.id())
                        ? new SeatSpec(s.id(), s.rowLabel(), s.number(), s.gridRow(), s.gridCol(), s.colSpan(),
                                s.capacity(), s.typeCode(), s.price(), false)
                        : s)
                .toList();
        return new SeatLayout(size.rows, gridCols, seats);
    }
}
