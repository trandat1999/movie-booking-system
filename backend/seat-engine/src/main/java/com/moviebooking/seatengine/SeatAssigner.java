package com.moviebooking.seatengine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Xếp từng người của nhóm vào các ghế nhóm đã mua theo sở thích chỗ ngồi (GRP22, PLAN.md M10).
 *
 * <p>Điểm của một cách xếp = {@value #NEXT_TO_POINTS} × số cặp "muốn ngồi cạnh" được ngồi liền nhau +
 * {@value #POSITION_POINTS} × số sở thích vị trí được đáp ứng. Khởi tạo theo thứ tự danh sách người (người gọi
 * đặt khách đi kèm ngay sau người mời) trên ghế sắp theo hàng rồi cột, sau đó leo đồi: thử hoán đổi mọi cặp ghế,
 * nhận hoán đổi làm tăng điểm, lặp tới khi không còn cải thiện. Với n ≤ 20 mỗi vòng ≤ 190 phép thử — tức thời,
 * và tất định (cùng đầu vào cho cùng kết quả).
 *
 * <p>Hai ghế "liền nhau" khi cùng hàng và ô của ghế sau bắt đầu ngay sau ghế trước. Ghế "đầu dãy" khi một bên không
 * có ghế (lối đi hoặc tường). Ghế "giữa" khi khoảng cách tới trục giữa phòng không lớn hơn trung vị các ghế của nhóm.
 */
public final class SeatAssigner {

    static final int NEXT_TO_POINTS = 10;
    static final int POSITION_POINTS = 4;

    /** Sở thích vị trí. */
    public enum Preference {
        NONE,
        /** Đầu dãy / cạnh lối đi (dễ ra vào). */
        AISLE,
        /** Giữa hàng (gần trục giữa màn hình). */
        CENTER
    }

    /**
     * Một người cần chỗ.
     *
     * @param key định danh duy nhất trong nhóm (vd. {@code "u12"}, {@code "u12#guest1"})
     * @param nextTo các {@code key} người này muốn ngồi cạnh (quan hệ không cần đối xứng)
     */
    public record Person(String key, Preference preference, Set<String> nextTo) {
        public Person {
            Objects.requireNonNull(key, "key");
            preference = preference == null ? Preference.NONE : preference;
            nextTo = nextTo == null ? Set.of() : Set.copyOf(nextTo);
        }
    }

    /**
     * Kết quả.
     *
     * @param seatByPerson ghế của từng người (theo thứ tự danh sách đầu vào); người vượt quá số ghế không có mặt
     * @param satisfied số mong muốn được đáp ứng
     * @param requested tổng số mong muốn (cặp ngồi cạnh + sở thích vị trí khác NONE) của những người được xếp
     */
    public record Assignment(Map<String, Long> seatByPerson, int satisfied, int requested) {}

    private SeatAssigner() {}

    public static Assignment assign(SeatLayout layout, Collection<Long> seatIds, List<Person> people) {
        Map<Long, SeatSpec> byId = new HashMap<>();
        layout.seats().forEach(s -> byId.put(s.id(), s));
        List<SeatSpec> seats = seatIds.stream()
                .map(id -> Objects.requireNonNull(byId.get(id), () -> "Unknown seat " + id))
                .sorted(Comparator.comparingInt(SeatSpec::gridRow).thenComparingInt(SeatSpec::gridCol))
                .toList();
        List<Person> seated = people.subList(0, Math.min(people.size(), seats.size()));
        Model model = new Model(layout, seats, seated);

        // slot[i] = chỉ số người ngồi ghế i, -1 = ghế trống
        int[] slot = new int[seats.size()];
        Arrays.fill(slot, -1);
        for (int i = 0; i < seated.size(); i++) {
            slot[i] = i;
        }
        int score = model.score(slot);
        boolean improved = true;
        while (improved) {
            improved = false;
            for (int a = 0; a < slot.length; a++) {
                for (int b = a + 1; b < slot.length; b++) {
                    if (slot[a] == slot[b]) {
                        continue;
                    }
                    swap(slot, a, b);
                    int candidate = model.score(slot);
                    if (candidate > score) {
                        score = candidate;
                        improved = true;
                    } else {
                        swap(slot, a, b);
                    }
                }
            }
        }

        Map<String, Long> result = new LinkedHashMap<>();
        Long[] seatOf = new Long[seated.size()];
        for (int i = 0; i < slot.length; i++) {
            if (slot[i] >= 0) {
                seatOf[slot[i]] = seats.get(i).id();
            }
        }
        for (int p = 0; p < seated.size(); p++) {
            result.put(seated.get(p).key(), seatOf[p]);
        }
        return new Assignment(result, model.satisfied(slot), model.requested());
    }

    private static void swap(int[] slot, int a, int b) {
        int t = slot[a];
        slot[a] = slot[b];
        slot[b] = t;
    }

    /** Dữ liệu tính sẵn để chấm điểm nhanh. */
    private static final class Model {
        private final List<Person> people;
        private final boolean[][] adjacent;
        private final boolean[] aisle;
        private final boolean[] central;
        /** Cặp chỉ số người (a, b) với a muốn ngồi cạnh b; mỗi cặp không thứ tự tính một lần. */
        private final List<int[]> pairs = new ArrayList<>();

        Model(SeatLayout layout, List<SeatSpec> seats, List<Person> people) {
            this.people = people;
            int n = seats.size();
            adjacent = new boolean[n][n];
            aisle = new boolean[n];
            central = new boolean[n];
            Map<Long, SeatSpec> cells = new HashMap<>();
            for (SeatSpec s : layout.seats()) {
                for (int c = s.gridCol(); c < s.colEnd(); c++) {
                    cells.put(cellKey(s.gridRow(), c), s);
                }
            }
            double axis = layout.gridCols() / 2.0;
            double[] distance = new double[n];
            for (int i = 0; i < n; i++) {
                SeatSpec s = seats.get(i);
                boolean leftEmpty = !cells.containsKey(cellKey(s.gridRow(), s.gridCol() - 1));
                boolean rightEmpty = !cells.containsKey(cellKey(s.gridRow(), s.colEnd()));
                aisle[i] = leftEmpty || rightEmpty;
                distance[i] = Math.abs(s.centerX() - axis);
                for (int j = 0; j < n; j++) {
                    SeatSpec t = seats.get(j);
                    adjacent[i][j] = s.gridRow() == t.gridRow()
                            && (s.colEnd() == t.gridCol() || t.colEnd() == s.gridCol());
                }
            }
            double[] sorted = distance.clone();
            Arrays.sort(sorted);
            double median = n == 0 ? 0 : sorted[(n - 1) / 2];
            for (int i = 0; i < n; i++) {
                central[i] = distance[i] <= median + 1e-9;
            }
            Map<String, Integer> index = new HashMap<>();
            for (int p = 0; p < people.size(); p++) {
                index.put(people.get(p).key(), p);
            }
            for (int p = 0; p < people.size(); p++) {
                int a = p;
                for (String other : people.get(a).nextTo()) {
                    Integer b = index.get(other);
                    if (b != null && b != a && pairs.stream().noneMatch(pair -> pair[0] == b && pair[1] == a)) {
                        pairs.add(new int[] {a, b});
                    }
                }
            }
        }

        int requested() {
            return pairs.size() + (int) people.stream().filter(p -> p.preference() != Preference.NONE).count();
        }

        int score(int[] slot) {
            return score(slot, NEXT_TO_POINTS, POSITION_POINTS);
        }

        int satisfied(int[] slot) {
            return score(slot, 1, 1);
        }

        private int score(int[] slot, int nextToPoints, int positionPoints) {
            int[] seatOf = new int[people.size()];
            for (int i = 0; i < slot.length; i++) {
                if (slot[i] >= 0) {
                    seatOf[slot[i]] = i;
                }
            }
            int total = 0;
            for (int[] pair : pairs) {
                if (adjacent[seatOf[pair[0]]][seatOf[pair[1]]]) {
                    total += nextToPoints;
                }
            }
            for (int p = 0; p < people.size(); p++) {
                boolean ok = switch (people.get(p).preference()) {
                    case AISLE -> aisle[seatOf[p]];
                    case CENTER -> central[seatOf[p]];
                    case NONE -> false;
                };
                if (ok) {
                    total += positionPoints;
                }
            }
            return total;
        }

        private static long cellKey(int row, int col) {
            return ((long) row << 32) | (col & 0xffffffffL);
        }
    }
}
