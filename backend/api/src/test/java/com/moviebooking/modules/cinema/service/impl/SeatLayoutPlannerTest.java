package com.moviebooking.modules.cinema.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moviebooking.modules.cinema.service.impl.SeatLayoutPlanner.Cell;
import com.moviebooking.modules.cinema.service.impl.SeatLayoutPlanner.PlannedSeat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SeatLayoutPlannerTest {

    private static final Map<String, Integer> SPANS = Map.of("STANDARD", 1, "VIP", 1, "COUPLE", 2);

    @Test
    void numbersSeatsLeftToRightSkippingAisles() {
        // hàng 0: ghế ở cột 0,1 | lối đi cột 2 | ghế cột 3,4
        List<PlannedSeat> seats = SeatLayoutPlanner.plan(
                1, 5, List.of(cell(0, 4), cell(0, 0), cell(0, 3), cell(0, 1)), SPANS);

        assertThat(seats).extracting(s -> s.rowLabel() + s.number() + "@" + s.gridCol())
                .containsExactly("A1@0", "A2@1", "A3@3", "A4@4");
    }

    @Test
    void emptyGridRowIsHorizontalAisleAndGetsNoLetter() {
        List<PlannedSeat> seats = SeatLayoutPlanner.plan(3, 2, List.of(cell(0, 0), cell(2, 0), cell(2, 1)), SPANS);

        assertThat(seats).extracting(s -> s.rowLabel() + s.number() + "@" + s.gridRow())
                .containsExactly("A1@0", "B1@2", "B2@2");
    }

    @Test
    void coupleSeatOccupiesTwoCellsAndCountsAsOneNumber() {
        List<PlannedSeat> seats = SeatLayoutPlanner.plan(
                1, 5, List.of(new Cell(0, 0, "COUPLE"), new Cell(0, 2, "COUPLE"), cell(0, 4)), SPANS);

        assertThat(seats).extracting(PlannedSeat::number).containsExactly(1, 2, 3);
        assertThat(seats.get(0).colSpan()).isEqualTo(2);
    }

    @Test
    void rejectsOverlapOutOfBoundsAndUnknownTypeWithAllErrors() {
        assertThatThrownBy(() -> SeatLayoutPlanner.plan(
                        2,
                        3,
                        List.of(
                                new Cell(0, 0, "COUPLE"),
                                cell(0, 1), // trùng nửa phải ghế đôi
                                new Cell(1, 2, "COUPLE"), // tràn ra ngoài lưới
                                new Cell(1, 0, "BED")),
                        SPANS))
                .isInstanceOfSatisfying(SeatLayoutPlanner.InvalidLayoutException.class, ex -> assertThat(ex.errors())
                        .hasSize(3)
                        .anyMatch(e -> e.contains("nhiều ghế"))
                        .anyMatch(e -> e.contains("ngoài lưới"))
                        .anyMatch(e -> e.contains("BED")));
    }

    @Test
    void rejectsEmptyLayout() {
        assertThatThrownBy(() -> SeatLayoutPlanner.plan(2, 2, List.of(), SPANS))
                .isInstanceOf(SeatLayoutPlanner.InvalidLayoutException.class);
    }

    @Test
    void rowLabelsContinuePastZ() {
        assertThat(SeatLayoutPlanner.rowLabel(0)).isEqualTo("A");
        assertThat(SeatLayoutPlanner.rowLabel(25)).isEqualTo("Z");
        assertThat(SeatLayoutPlanner.rowLabel(26)).isEqualTo("AA");
        assertThat(SeatLayoutPlanner.rowLabel(27)).isEqualTo("AB");
    }

    private static Cell cell(int row, int col) {
        return new Cell(row, col, "STANDARD");
    }
}
