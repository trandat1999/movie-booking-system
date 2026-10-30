package com.moviebooking.modules.booking.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import com.moviebooking.modules.booking.service.impl.OrphanSeatChecker.RowSeat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class OrphanSeatCheckerTest {

    /** Hàng A: A1..A5 liền nhau (cột 0–4) | lối đi cột 5 | A6, A7 (cột 6–7). */
    private static List<RowSeat> rowA(Set<Integer> unavailable) {
        List<RowSeat> seats = new ArrayList<>();
        int[] cols = {0, 1, 2, 3, 4, 6, 7};
        for (int i = 0; i < cols.length; i++) {
            long id = i + 1;
            seats.add(new RowSeat(id, "A" + id, 0, cols[i], 1, !unavailable.contains((int) id)));
        }
        return seats;
    }

    @Test
    void leavingOneSeatAtTheRowEdgeIsAnOrphan() {
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of()), Set.of(2L, 3L))).containsExactly("A1");
    }

    @Test
    void leavingOneSeatBetweenSelectionAndSoldSeatIsAnOrphan() {
        // A4 đã bán; chọn A2 → A3 bị kẹt giữa A2 và A4, A1 bị kẹt ở mép
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of(4)), Set.of(2L))).containsExactlyInAnyOrder("A1", "A3");
    }

    @Test
    void contiguousSelectionWithoutGapsIsFine() {
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of()), Set.of(1L, 2L))).isEmpty();
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of()), Set.of(6L, 7L))).isEmpty();
    }

    @Test
    void aisleSeparatesBlocks() {
        // Chọn A5 (sát lối đi): A6 ở dãy khác nên không bị coi là kẹt; A4 vẫn còn A1–A4 liền nhau
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of()), Set.of(5L))).isEmpty();
        // Chọn A6 → A7 một mình ở mép dãy
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of()), Set.of(6L))).containsExactly("A7");
    }

    @Test
    void preExistingIsolatedSeatIsIgnored() {
        // A2 đã bán từ trước nên A1 vốn đã lẻ; chọn A3, A4, A5 không gây thêm ghế lẻ
        assertThat(OrphanSeatChecker.findOrphans(rowA(Set.of(2)), Set.of(3L, 4L, 5L))).isEmpty();
    }

    @Test
    void coupleSeatCountsAsOneSeat() {
        // Ghế đôi D1 chiếm cột 0–1, D2 cột 2–3, D3 cột 4: chọn D1 + D2 để lại D3 lẻ ở mép
        List<RowSeat> row = List.of(
                new RowSeat(1L, "D1", 3, 0, 2, true), new RowSeat(2L, "D2", 3, 2, 2, true), new RowSeat(3L, "D3", 3, 4, 1, true));
        assertThat(OrphanSeatChecker.findOrphans(row, Set.of(1L, 2L))).containsExactly("D3");
        assertThat(OrphanSeatChecker.findOrphans(row, Set.of(2L, 3L))).containsExactly("D1");
    }
}
