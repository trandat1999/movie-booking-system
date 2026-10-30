package com.moviebooking.seatengine;

import com.moviebooking.seatengine.SuggestionResult.NoSuggestionReason;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Đề xuất cụm ghế cho nhóm n người (PLAN.md 8.2). Không trạng thái, an toàn đa luồng.
 *
 * <ol>
 *   <li>Tìm đoạn ghế dùng được liền kề trên từng hàng.
 *   <li>Ứng viên một hàng: cửa sổ trượt tối thiểu có ≥ n chỗ.
 *   <li>Ứng viên nhiều hàng (khi {@code allowSplit}): k hàng liên tiếp, n chia đều thành k phần (mọi hoán vị),
 *       các cửa sổ phải chồng cột với hàng trước; beam search giữ {@value #BEAM_WIDTH} trạng thái tốt nhất mỗi bước.
 *   <li>Chấm điểm, sắp xếp, chọn top-K đa dạng (không trùng quá 50% ghế), kèm giải thích.
 * </ol>
 */
public final class SeatSuggester {

    static final int BEAM_WIDTH = 50;
    /** Hai phương án trùng quá tỷ lệ ghế này thì chỉ giữ phương án điểm cao hơn. */
    static final double MAX_OVERLAP = 0.5;

    public SuggestionResult suggest(SeatLayout layout, SuggestionRequest request) {
        List<SeatRow> rows = buildRows(layout, request);
        int n = request.partySize();
        int eligibleCapacity = rows.stream().mapToInt(SeatRow::eligibleCapacity).sum();
        if (eligibleCapacity < n) {
            return new SuggestionResult(List.of(), NoSuggestionReason.NOT_ENOUGH_SEATS);
        }

        Scorer scorer = new Scorer(layout, request);
        WindowCache windows = new WindowCache(request);
        Map<String, Candidate> candidates = new HashMap<>();

        for (SeatRow row : rows) {
            for (Window w : windows.get(row, n)) {
                add(candidates, List.of(w), scorer.score(List.of(w), n));
            }
        }
        if (request.allowSplit()) {
            int maxRows = Math.min(Math.min(request.maxRows(), n), rows.size());
            for (int k = 2; k <= maxRows; k++) {
                for (int[] parts : balancedSplits(n, k)) {
                    for (int start = 0; start + k <= rows.size(); start++) {
                        beamSearch(rows.subList(start, start + k), parts, windows, scorer)
                                .forEach(windowsOfCandidate ->
                                        add(candidates, windowsOfCandidate, scorer.score(windowsOfCandidate, n)));
                    }
                }
            }
        }

        List<Candidate> ranked = new ArrayList<>(candidates.values());
        // Cùng điểm: ít hàng hơn trước, rồi theo mã ghế để kết quả ổn định
        ranked.sort(Comparator.comparingDouble(Candidate::score)
                .reversed()
                .thenComparingInt((Candidate c) -> c.windows.size())
                .thenComparing((Candidate c) -> c.key));
        List<Suggestion> picked = pickDiverse(ranked, request.topK());
        return picked.isEmpty()
                ? new SuggestionResult(List.of(), NoSuggestionReason.NO_CLUSTER)
                : new SuggestionResult(picked, null);
    }

    /**
     * Chấm điểm một tập ghế bất kỳ theo cùng hàm điểm (lựa chọn thủ công của trưởng nhóm, kết quả của baseline).
     * Ghế rời rạc trong cùng hàng tạo thành nhiều cụm; mỗi cụm thêm bị trừ điểm như thêm một hàng.
     *
     * @throws IllegalArgumentException nếu có ghế không tồn tại / không còn trống, hoặc tổng chỗ ít hơn số người
     */
    public Suggestion evaluate(SeatLayout layout, SuggestionRequest request, Collection<Long> seatIds) {
        List<SeatRow> rows = buildRows(layout, request);
        Map<Long, int[]> position = new HashMap<>();
        for (SeatRow row : rows) {
            for (int i = 0; i < row.seats.length; i++) {
                position.put(row.seats[i].id(), new int[] {row.index, i});
            }
        }
        Map<Integer, boolean[]> selectedByRow = new TreeMap<>();
        for (Long id : new HashSet<>(seatIds)) {
            int[] pos = position.get(id);
            if (pos == null || !rows.get(pos[0]).seats[pos[1]].available()) {
                throw new IllegalArgumentException("Seat " + id + " is unknown or not available");
            }
            selectedByRow.computeIfAbsent(pos[0], r -> new boolean[rows.get(r).seats.length])[pos[1]] = true;
        }
        if (selectedByRow.isEmpty()) {
            throw new IllegalArgumentException("No seats selected");
        }

        List<Window> windows = new ArrayList<>();
        int orphans = 0;
        for (Map.Entry<Integer, boolean[]> entry : selectedByRow.entrySet()) {
            SeatRow row = rows.get(entry.getKey());
            boolean[] selected = entry.getValue();
            orphans += row.orphansCreatedBy(selected);
            for (int i = 0; i < selected.length; i++) {
                if (selected[i]) {
                    int from = i;
                    while (i + 1 < selected.length && selected[i + 1] && row.adjacentNext[i]) {
                        i++;
                    }
                    windows.add(new Window(row, from, i, request));
                }
            }
        }
        int capacity = windows.stream().mapToInt(w -> w.capacity).sum();
        if (capacity < request.partySize()) {
            throw new IllegalArgumentException("Selected seats hold fewer people than the party size");
        }
        Scorer scorer = new Scorer(layout, request);
        return new Candidate(windows, scorer.score(windows, request.partySize(), orphans)).toSuggestion();
    }

    private static List<SeatRow> buildRows(SeatLayout layout, SuggestionRequest request) {
        Map<Integer, List<SeatSpec>> byRow = new TreeMap<>();
        layout.seats().forEach(seat -> byRow.computeIfAbsent(seat.gridRow(), r -> new ArrayList<>()).add(seat));
        List<SeatRow> rows = new ArrayList<>(byRow.size());
        for (List<SeatSpec> seats : byRow.values()) {
            rows.add(new SeatRow(rows.size(), seats, request));
        }
        return rows;
    }

    /** Các cách chia n thành k phần chênh nhau tối đa 1 (mọi hoán vị phân biệt), ví dụ 5, 2 → [3,2], [2,3]. */
    static List<int[]> balancedSplits(int n, int k) {
        int base = n / k;
        int extra = n % k;
        List<int[]> result = new ArrayList<>();
        permute(new int[k], 0, extra, k - extra, base, result);
        return result;
    }

    private static void permute(int[] current, int index, int bigLeft, int smallLeft, int base, List<int[]> out) {
        if (index == current.length) {
            out.add(current.clone());
            return;
        }
        if (bigLeft > 0) {
            current[index] = base + 1;
            permute(current, index + 1, bigLeft - 1, smallLeft, base, out);
        }
        if (smallLeft > 0) {
            current[index] = base;
            permute(current, index + 1, bigLeft, smallLeft - 1, base, out);
        }
    }

    /** Dựng phương án từng hàng một; sau mỗi hàng chỉ giữ {@value #BEAM_WIDTH} trạng thái điểm cao nhất. */
    private static List<List<Window>> beamSearch(
            List<SeatRow> rows, int[] parts, WindowCache cache, Scorer scorer) {
        List<List<Window>> beam = new ArrayList<>();
        for (Window w : cache.get(rows.get(0), parts[0])) {
            beam.add(List.of(w));
        }
        int people = parts[0];
        for (int i = 1; i < rows.size() && !beam.isEmpty(); i++) {
            people += parts[i];
            List<Window> options = cache.get(rows.get(i), parts[i]);
            List<Partial> next = new ArrayList<>();
            for (List<Window> state : beam) {
                Window last = state.get(state.size() - 1);
                for (Window w : options) {
                    if (w.overlapsColumns(last)) {
                        List<Window> extended = new ArrayList<>(state.size() + 1);
                        extended.addAll(state);
                        extended.add(w);
                        next.add(new Partial(extended, scorer.score(extended, people).score()));
                    }
                }
            }
            next.sort(Comparator.comparingDouble(Partial::score).reversed());
            beam = next.stream().limit(BEAM_WIDTH).map(Partial::windows).toList();
        }
        return beam;
    }

    private static void add(Map<String, Candidate> candidates, List<Window> windows, Scorer.Scored scored) {
        Candidate candidate = new Candidate(windows, scored);
        candidates.merge(candidate.key, candidate, (a, b) -> a.score() >= b.score() ? a : b);
    }

    private static List<Suggestion> pickDiverse(List<Candidate> ranked, int topK) {
        List<Candidate> picked = new ArrayList<>();
        for (Candidate candidate : ranked) {
            if (picked.size() == topK) {
                break;
            }
            if (picked.stream().noneMatch(p -> overlap(p, candidate) > MAX_OVERLAP)) {
                picked.add(candidate);
            }
        }
        return picked.stream().map(Candidate::toSuggestion).toList();
    }

    private static double overlap(Candidate a, Candidate b) {
        int common = 0;
        for (Long id : b.seatIds) {
            if (a.seatIdSet.contains(id)) {
                common++;
            }
        }
        return (double) common / Math.min(a.seatIds.size(), b.seatIds.size());
    }

    private record Partial(List<Window> windows, double score) {}

    /** Cửa sổ của từng (hàng, số chỗ) — dùng lại giữa các ứng viên và các lượt beam search. */
    private static final class WindowCache {
        private final SuggestionRequest request;
        private final Map<Long, List<Window>> cache = new HashMap<>();

        WindowCache(SuggestionRequest request) {
            this.request = request;
        }

        List<Window> get(SeatRow row, int places) {
            return cache.computeIfAbsent(((long) row.index << 32) | places, k -> row.windows(places, request));
        }
    }

    private static final class Candidate {
        final List<Window> windows;
        final Scorer.Scored scored;
        final List<Long> seatIds = new ArrayList<>();
        final Set<Long> seatIdSet = new HashSet<>();
        final String key;

        Candidate(List<Window> windows, Scorer.Scored scored) {
            this.windows = windows;
            this.scored = scored;
            for (Window w : windows) {
                for (int i = w.from; i <= w.to; i++) {
                    seatIds.add(w.row.seats[i].id());
                }
            }
            seatIdSet.addAll(seatIds);
            this.key = seatIds.toString();
        }

        double score() {
            return scored.score();
        }

        Suggestion toSuggestion() {
            List<String> labels = new ArrayList<>(seatIds.size());
            int capacity = 0;
            long price = 0;
            for (Window w : windows) {
                for (int i = w.from; i <= w.to; i++) {
                    labels.add(w.row.seats[i].label());
                }
                capacity += w.capacity;
                price += w.price;
            }
            double rounded = Math.round(scored.score() * 10) / 10.0;
            return new Suggestion(
                    seatIds,
                    labels,
                    (int) windows.stream().map(w -> w.row).distinct().count(),
                    capacity,
                    price,
                    rounded,
                    scored.breakdown(),
                    Explainer.explain(windows, scored.breakdown()));
        }
    }
}
