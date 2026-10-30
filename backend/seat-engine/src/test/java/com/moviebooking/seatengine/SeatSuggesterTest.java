package com.moviebooking.seatengine;

import static com.moviebooking.seatengine.Layouts.id;
import static com.moviebooking.seatengine.Layouts.parse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moviebooking.seatengine.SuggestionResult.NoSuggestionReason;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SeatSuggesterTest {

    private final SeatSuggester suggester = new SeatSuggester();

    private SuggestionResult suggest(SeatLayout layout, SuggestionRequest request) {
        return suggester.suggest(layout, request);
    }

    @Test
    void emptyRoomPrefersCentralSingleRow() {
        SeatLayout layout = parse(
                "SSSSSSSSSS", //
                "SSSSSSSSSS",
                "SSSSSSSSSS",
                "SSSSSSSSSS",
                "SSSSSSSSSS",
                "SSSSSSSSSS",
                "SSSSSSSSSS");
        SuggestionResult result = suggest(layout, SuggestionRequest.of(4));

        Suggestion best = result.suggestions().get(0);
        assertThat(result.found()).isTrue();
        assertThat(result.reason()).isNull();
        assertThat(best.rowCount()).isEqualTo(1);
        // Hàng ~2/3 chiều sâu (E), giữa phòng (ghế 4–7)
        assertThat(best.labels()).containsExactly("E4", "E5", "E6", "E7");
        assertThat(best.capacity()).isEqualTo(4);
        assertThat(best.totalPrice()).isEqualTo(4 * Layouts.STANDARD_PRICE);
        assertThat(best.breakdown().orphanSeats()).isZero();
        assertThat(best.explanation()).startsWith("Cùng hàng E (E4–E7)").contains("vị trí trung tâm");
        assertThat(result.suggestions()).hasSize(5);
        assertThat(result.suggestions())
                .extracting(Suggestion::score)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void aisleSplitsRuns() {
        SeatLayout layout = parse("SS.SS", "SS.SS");
        assertThat(suggest(layout, SuggestionRequest.of(3).withAllowSplit(false)).reason())
                .isEqualTo(NoSuggestionReason.NO_CLUSTER);

        SuggestionResult split = suggest(layout, SuggestionRequest.of(3));
        Suggestion best = split.suggestions().get(0);
        assertThat(best.rowCount()).isEqualTo(2);
        assertThat(best.capacity()).isEqualTo(3);
        assertThat(best.explanation()).startsWith("Chia 2 hàng A–B").contains("thẳng cột");
        // Không cửa sổ nào vượt qua lối đi
        for (Suggestion s : split.suggestions()) {
            for (Long seatId : s.seatIds()) {
                assertThat(seatId % 100).isNotEqualTo(2);
            }
        }
    }

    @Test
    void coupleSeatIsNeverSplitAndWastedPlacesArePenalised() {
        SeatLayout layout = parse("C=C=C=");
        SuggestionResult two = suggest(layout, SuggestionRequest.of(2));
        assertThat(two.suggestions().get(0).seatIds()).hasSize(1);
        assertThat(two.suggestions().get(0).breakdown().wastedPlaces()).isZero();

        SuggestionResult three = suggest(layout, SuggestionRequest.of(3));
        Suggestion best = three.suggestions().get(0);
        assertThat(best.seatIds()).hasSize(2);
        assertThat(best.capacity()).isEqualTo(4);
        assertThat(best.breakdown().wastedPlaces()).isEqualTo(1);
        assertThat(best.explanation()).contains("dư 1 chỗ do ghế đôi");
    }

    @Test
    void avoidsLeavingOrphanSeats() {
        // Chọn 3 ghế giữa (A2–A4) để lại A1, A5 lẻ; chọn sát mép thì không
        SeatLayout layout = parse("SSSSS");
        SuggestionResult result = suggest(layout, SuggestionRequest.of(3));
        Suggestion best = result.suggestions().get(0);
        assertThat(best.breakdown().orphanSeats()).isZero();
        assertThat(best.labels()).isIn(List.of("A1", "A2", "A3"), List.of("A3", "A4", "A5"));
    }

    @Test
    void countsOrphanNextToSoldSeat() {
        // A1 trống bị kẹt giữa mép dãy và cửa sổ A2–A3; A4 đã bán
        SeatLayout layout = parse("SSSX");
        Suggestion only = suggest(layout, SuggestionRequest.of(2).withAllowSplit(false).withTopK(10))
                .suggestions()
                .stream()
                .filter(s -> s.labels().equals(List.of("A2", "A3")))
                .findFirst()
                .orElseThrow();
        assertThat(only.breakdown().orphanSeats()).isEqualTo(1);
        assertThat(only.explanation()).contains("để lại 1 ghế lẻ");
    }

    @Test
    void nearlyFullRoomStillFindsWhatIsLeft() {
        SeatLayout layout = parse(
                "XXSXXXXX", //
                "XXXXSSXX",
                "XXXXSSXX",
                "XSXXXXXX");
        SuggestionResult pair = suggest(layout, SuggestionRequest.of(2).withAllowSplit(false));
        assertThat(pair.suggestions()).extracting(Suggestion::labels)
                .contains(List.of("B5", "B6"), List.of("C5", "C6"));

        SuggestionResult four = suggest(layout, SuggestionRequest.of(4));
        assertThat(four.suggestions().get(0).labels()).containsExactly("B5", "B6", "C5", "C6");

        assertThat(suggest(layout, SuggestionRequest.of(4).withAllowSplit(false)).reason())
                .isEqualTo(NoSuggestionReason.NO_CLUSTER);
    }

    @Test
    void partyLargerThanAnyRowIsSplitAcrossAlignedRows() {
        SeatLayout layout = parse("SSSS", "SSSS", "SSSS");
        SuggestionResult result = suggest(layout, SuggestionRequest.of(6));
        Suggestion best = result.suggestions().get(0);
        assertThat(best.rowCount()).isEqualTo(2);
        assertThat(best.capacity()).isEqualTo(6);
        assertThat(best.breakdown().misalignment()).isLessThan(0.5);

        assertThat(suggest(layout, SuggestionRequest.of(6).withAllowSplit(false)).reason())
                .isEqualTo(NoSuggestionReason.NO_CLUSTER);
    }

    @Test
    void maxRowsIsRespected() {
        SeatLayout layout = parse("SS", "SS", "SS", "SS");
        assertThat(suggest(layout, SuggestionRequest.of(6).withMaxRows(2)).reason())
                .isEqualTo(NoSuggestionReason.NO_CLUSTER);
        Suggestion best = suggest(layout, SuggestionRequest.of(6).withMaxRows(3)).suggestions().get(0);
        assertThat(best.rowCount()).isEqualTo(3);
    }

    @Test
    void splitRowsMustShareColumns() {
        SeatLayout layout = parse("SS....", "....SS");
        assertThat(suggest(layout, SuggestionRequest.of(4)).reason()).isEqualTo(NoSuggestionReason.NO_CLUSTER);
    }

    @Test
    void notEnoughSeats() {
        SeatLayout layout = parse("SXXS", "XXXX");
        assertThat(suggest(layout, SuggestionRequest.of(3)).reason()).isEqualTo(NoSuggestionReason.NOT_ENOUGH_SEATS);
        assertThat(suggest(layout, SuggestionRequest.of(3)).suggestions()).isEmpty();
    }

    @Test
    void allowedSeatTypesFilterSeats() {
        SeatLayout layout = parse("SSVVSS");
        Suggestion vip = suggest(layout, SuggestionRequest.of(2).withAllowedSeatTypes(Set.of("VIP")))
                .suggestions()
                .get(0);
        assertThat(vip.labels()).containsExactly("A3", "A4");
        assertThat(suggest(layout, SuggestionRequest.of(3).withAllowedSeatTypes(Set.of("VIP"))).reason())
                .isEqualTo(NoSuggestionReason.NOT_ENOUGH_SEATS);
    }

    @Test
    void budgetIsSoftPenalty() {
        SeatLayout layout = parse("SSVVSS");
        SuggestionRequest request = SuggestionRequest.of(2).withTopK(10);
        Suggestion free = central(suggest(layout, request));
        Suggestion capped = central(suggest(layout, request.withMaxPricePerPerson(Layouts.STANDARD_PRICE)));
        // VIP 100k so với ngân sách 80k/người: vượt 40k / 160k = 0,25 → trừ 0,25 × trọng số ngân sách, không bị loại
        assertThat(capped.breakdown().overBudget()).isEqualTo(0.25);
        assertThat(free.score() - capped.score())
                .isCloseTo(0.25 * ScoringWeights.DEFAULT.budget(), org.assertj.core.data.Offset.offset(0.1));
        assertThat(capped.explanation()).contains("vượt ngân sách");
        assertThat(free.explanation()).doesNotContain("vượt ngân sách");
    }

    private static Suggestion central(SuggestionResult result) {
        return result.suggestions().stream()
                .filter(s -> s.labels().equals(List.of("A3", "A4")))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void suggestionsAreDiverse() {
        SeatLayout layout = parse("SSSSSSSSSSSS", "SSSSSSSSSSSS", "SSSSSSSSSSSS", "SSSSSSSSSSSS");
        List<Suggestion> suggestions = suggest(layout, SuggestionRequest.of(4).withTopK(8)).suggestions();
        assertThat(suggestions).hasSize(8);
        for (int i = 0; i < suggestions.size(); i++) {
            for (int j = i + 1; j < suggestions.size(); j++) {
                Set<Long> common = new HashSet<>(suggestions.get(i).seatIds());
                common.retainAll(suggestions.get(j).seatIds());
                assertThat(common.size()).isLessThanOrEqualTo(2);
            }
        }
    }

    @Test
    void topKLimitsResultCount() {
        SeatLayout layout = parse("SSSSSSSSSS", "SSSSSSSSSS");
        assertThat(suggest(layout, SuggestionRequest.of(2).withTopK(1)).suggestions()).hasSize(1);
    }

    @Test
    void singlePersonGetsSingleSeat() {
        SeatLayout layout = parse("XXSXX", "XSXSX");
        Suggestion best = suggest(layout, SuggestionRequest.of(1)).suggestions().get(0);
        assertThat(best.seatIds()).hasSize(1);
        assertThat(best.explanation()).startsWith("Cùng hàng").doesNotContain("–");
    }

    @Test
    void resultIsDeterministic() {
        SeatLayout layout = RandomRooms.room(new Random(7), 15, 20, 0.6);
        SuggestionRequest request = SuggestionRequest.of(5);
        assertThat(suggest(layout, request)).isEqualTo(suggest(layout, request));
    }

    @Test
    void largeRoomIsFast() {
        Random random = new Random(42);
        List<Long> durations = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            SeatLayout layout = RandomRooms.room(random, 15, 20, 0.6);
            long start = System.nanoTime();
            suggest(layout, SuggestionRequest.of(10));
            durations.add(System.nanoTime() - start);
        }
        durations.sort(null);
        // Ngưỡng rộng để không chập chờn trên máy CI; số liệu chính xác đo bằng JMH ở seat-bench
        assertThat(durations.get(durations.size() / 2) / 1_000_000).isLessThan(200);
    }

    @Test
    void evaluateScoresEngineSuggestionIdentically() {
        SeatLayout layout = RandomRooms.room(new Random(3), 10, 16, 0.4);
        SuggestionRequest request = SuggestionRequest.of(5);
        for (Suggestion s : suggest(layout, request).suggestions()) {
            Suggestion evaluated = suggester.evaluate(layout, request, s.seatIds());
            assertThat(evaluated.score()).isEqualTo(s.score());
            assertThat(evaluated.labels()).isEqualTo(s.labels());
        }
    }

    @Test
    void evaluateHandlesScatteredSelection() {
        SeatLayout layout = parse("SSSSS", "SSSSS");
        Suggestion scattered = suggester.evaluate(layout, SuggestionRequest.of(2), List.of(id(0, 0), id(0, 2)));
        assertThat(scattered.rowCount()).isEqualTo(1);
        assertThat(scattered.breakdown().orphanSeats()).isEqualTo(1); // A2 kẹt giữa hai ghế đã chọn
        assertThat(scattered.explanation()).startsWith("Tách 2 cụm trên 1 hàng");

        Suggestion together = suggester.evaluate(layout, SuggestionRequest.of(2), List.of(id(0, 0), id(0, 1)));
        assertThat(together.score()).isGreaterThan(scattered.score());
    }

    @Test
    void evaluateRejectsInvalidSelection() {
        SeatLayout layout = parse("SXS");
        SuggestionRequest two = SuggestionRequest.of(2);
        assertThatThrownBy(() -> suggester.evaluate(layout, two, List.of(id(0, 0), id(0, 1))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suggester.evaluate(layout, two, List.of(id(5, 5))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suggester.evaluate(layout, two, List.of(id(0, 0))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> suggester.evaluate(layout, two, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void balancedSplitsCoverAllOrders() {
        assertThat(SeatSuggester.balancedSplits(5, 2)).containsExactly(new int[] {3, 2}, new int[] {2, 3});
        assertThat(SeatSuggester.balancedSplits(6, 3)).containsExactly(new int[] {2, 2, 2});
        assertThat(SeatSuggester.balancedSplits(7, 3)).hasSize(3);
    }

    @Test
    void validatesInput() {
        assertThatThrownBy(() -> SuggestionRequest.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SuggestionRequest.of(2).withTopK(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SuggestionRequest.of(2).withMaxPricePerPerson(0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScoringWeights(-1, 0, 0, 0, 0, 0)).isInstanceOf(IllegalArgumentException.class);
        SeatSpec seat = new SeatSpec(1, "A", 1, 0, 0, 1, 1, "STANDARD", 1, true);
        assertThatThrownBy(() -> new SeatLayout(1, 1, List.of(seat, seat))).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeatLayout(1, 1, List.of(new SeatSpec(2, "A", 1, 0, 1, 1, 1, "S", 1, true))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeatSpec(3, "A", 1, 0, 0, 0, 1, "S", 1, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SeatLayout(0, 1, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> parse("Q")).isInstanceOf(IllegalArgumentException.class);
        assertThat(id(1, 2)).isEqualTo(102);
    }
}
