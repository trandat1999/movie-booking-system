package com.moviebooking.seatbench;

import static org.assertj.core.api.Assertions.assertThat;

import com.moviebooking.seatbench.RoomGenerator.FillPattern;
import com.moviebooking.seatbench.RoomGenerator.RoomSize;
import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSpec;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SeatBenchTest {

    @Test
    void generatorProducesExactSizeAndFillRate() {
        for (RoomSize size : RoomSize.values()) {
            for (FillPattern pattern : FillPattern.values()) {
                SeatLayout layout = RoomGenerator.generate(size, 0.6, pattern, new Random(1));
                long sold = layout.seats().stream().filter(s -> !s.available()).count();
                assertThat(layout.seats()).hasSize(size.seats());
                assertThat(sold).isEqualTo(Math.round(0.6 * size.seats()));
            }
        }
    }

    @Test
    void centerPatternSellsCentralSeatsFirst() {
        SeatLayout layout = RoomGenerator.generate(RoomSize.S300, 0.3, FillPattern.CENTER, new Random(5));
        double sweetX = layout.gridCols() / 2.0;
        double soldDistance = layout.seats().stream().filter(s -> !s.available())
                .mapToDouble(s -> Math.abs(s.gridCol() - sweetX)).average().orElseThrow();
        double freeDistance = layout.seats().stream().filter(SeatSpec::available)
                .mapToDouble(s -> Math.abs(s.gridCol() - sweetX)).average().orElseThrow();
        assertThat(soldDistance).isLessThan(freeDistance);
    }

    @Test
    void everyStrategyPicksExactlyNFreeSeats() {
        SeatLayout layout = RoomGenerator.generate(RoomSize.S200, 0.6, FillPattern.RANDOM, new Random(9));
        for (Strategy strategy : Strategy.ALL) {
            List<Long> chosen = strategy.choose(layout, 4);
            assertThat(new HashSet<>(chosen)).hasSize(4);
            assertThat(chosen).allMatch(id -> layout.seats().stream()
                    .anyMatch(s -> s.id() == id && s.available()));
        }
    }

    @Test
    void strategiesReturnEmptyWhenRoomIsFull() {
        SeatLayout layout = RoomGenerator.generate(RoomSize.S100, 1.0, FillPattern.RANDOM, new Random(2));
        for (Strategy strategy : Strategy.ALL) {
            assertThat(strategy.choose(layout, 2)).isEmpty();
        }
    }

    @Test
    void fragmentsCountsContiguousGroups() {
        SeatLayout layout = RoomGenerator.generate(RoomSize.S100, 0, FillPattern.RANDOM, new Random(3));
        // Hàng 0: lối đi ở cột 2 → ghế cột 0,1 liền nhau; cột 3 ở khối khác
        assertThat(Experiment.fragments(layout, List.of(0L, 1L))).isEqualTo(1);
        assertThat(Experiment.fragments(layout, List.of(0L, 1L, 3L))).isEqualTo(2);
        assertThat(Experiment.fragments(layout, List.of(0L, 100L))).isEqualTo(2);
    }

    @Test
    void svgChartRenders() {
        String svg = SvgBarChart.render("T", List.of("a", "b"), List.of("x", "y"), new double[][] {{1, 2}, {3, 4}}, 5, "%.0f");
        assertThat(svg).startsWith("<svg").contains("<rect").endsWith("</svg>\n");
    }
}
