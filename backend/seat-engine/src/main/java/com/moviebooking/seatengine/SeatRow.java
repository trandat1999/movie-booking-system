package com.moviebooking.seatengine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Một hàng ghế đã sắp theo cột, kèm thông tin liền kề và các <b>đoạn</b> (run): dãy ghế dùng được liền kề tối đa
 * (PLAN.md 8.2 bước 1, O(S)).
 */
final class SeatRow {

    final int index;
    final int gridRow;
    final String label;
    final SeatSpec[] seats;
    /** {@code adjacentNext[i]}: ghế i và i+1 liền kề (không có lối đi ở giữa). */
    final boolean[] adjacentNext;
    /** Ghế còn trống và thuộc loại được phép. */
    final boolean[] eligible;
    /** Mỗi phần tử {@code {from, to}} (chỉ số trong {@link #seats}, gồm cả hai đầu). */
    final List<int[]> runs = new ArrayList<>();

    SeatRow(int index, List<SeatSpec> rowSeats, SuggestionRequest request) {
        this.index = index;
        List<SeatSpec> sorted = new ArrayList<>(rowSeats);
        sorted.sort(Comparator.comparingInt(SeatSpec::gridCol));
        this.seats = sorted.toArray(SeatSpec[]::new);
        this.gridRow = seats[0].gridRow();
        this.label = seats[0].rowLabel();
        this.adjacentNext = new boolean[seats.length];
        this.eligible = new boolean[seats.length];
        for (int i = 0; i < seats.length; i++) {
            eligible[i] = seats[i].available() && request.allows(seats[i]);
            adjacentNext[i] = i + 1 < seats.length && seats[i + 1].gridCol() == seats[i].colEnd();
        }
        int start = -1;
        for (int i = 0; i < seats.length; i++) {
            if (eligible[i] && start < 0) {
                start = i;
            }
            boolean runEnds = !eligible[i] || !adjacentNext[i];
            if (start >= 0 && runEnds) {
                runs.add(new int[] {start, eligible[i] ? i : i - 1});
                start = -1;
            }
        }
    }

    int eligibleCapacity() {
        int total = 0;
        for (int i = 0; i < seats.length; i++) {
            if (eligible[i]) {
                total += seats[i].capacity();
            }
        }
        return total;
    }

    /**
     * Các cửa sổ tối thiểu chứa ít nhất {@code places} chỗ: với mỗi ghế bắt đầu trong một đoạn, mở rộng sang phải
     * tới khi đủ chỗ. Chỉ thừa chỗ khi ghế cuối là ghế đôi.
     */
    List<Window> windows(int places, SuggestionRequest request) {
        List<Window> result = new ArrayList<>();
        for (int[] run : runs) {
            for (int from = run[0]; from <= run[1]; from++) {
                int capacity = 0;
                int to = from;
                while (to <= run[1] && capacity < places) {
                    capacity += seats[to].capacity();
                    to++;
                }
                if (capacity < places) {
                    break; // các ghế bắt đầu sau còn ít chỗ hơn
                }
                result.add(new Window(this, from, to - 1, request));
            }
        }
        return result;
    }

    /**
     * Số ghế lẻ tạo ra khi chọn một tập ghế bất kỳ trong hàng (có thể rời rạc) — dùng khi chấm điểm lựa chọn thủ
     * công hoặc của baseline.
     */
    int orphansCreatedBy(boolean[] selected) {
        int orphans = 0;
        for (int i = 0; i < seats.length; i++) {
            if (selected[i] || !seats[i].available()) {
                continue;
            }
            boolean leftAdjacent = i > 0 && adjacentNext[i - 1];
            boolean rightAdjacent = adjacentNext[i];
            boolean besideSelection = (leftAdjacent && selected[i - 1]) || (rightAdjacent && selected[i + 1]);
            boolean leftTaken = !leftAdjacent || selected[i - 1] || !seats[i - 1].available();
            boolean rightTaken = !rightAdjacent || selected[i + 1] || !seats[i + 1].available();
            if (besideSelection && leftTaken && rightTaken) {
                orphans++;
            }
        }
        return orphans;
    }

    /** Ghế i còn trống sau khi chọn cửa sổ [from, to] (tính cả ghế khác loại được phép). */
    private boolean freeAfter(int i, int from, int to) {
        return seats[i].available() && (i < from || i > to);
    }

    /**
     * Số ghế lẻ cửa sổ tạo ra (BR14): ghế trống ngay sát cửa sổ bị kẹt giữa cửa sổ và ghế không trống / mép dãy.
     * Chỉ hai ghế sát hai đầu cửa sổ có thể trở thành ghế lẻ do lựa chọn này.
     */
    int orphansCreatedBy(int from, int to) {
        int orphans = 0;
        int left = from - 1;
        if (left >= 0 && adjacentNext[left] && freeAfter(left, from, to)) {
            boolean outerTaken = left == 0 || !adjacentNext[left - 1] || !freeAfter(left - 1, from, to);
            if (outerTaken) {
                orphans++;
            }
        }
        int right = to + 1;
        if (right < seats.length && adjacentNext[to] && freeAfter(right, from, to)) {
            boolean outerTaken =
                    right == seats.length - 1 || !adjacentNext[right] || !freeAfter(right + 1, from, to);
            if (outerTaken) {
                orphans++;
            }
        }
        return orphans;
    }
}
