package com.moviebooking.seatbench;

import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSpec;
import com.moviebooking.seatengine.SeatSuggester;
import com.moviebooking.seatengine.SuggestionRequest;
import com.moviebooking.seatengine.SuggestionResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Một cách chọn ghế cho nhóm n người, để so sánh trong thực nghiệm. */
public interface Strategy {

    String name();

    /** @return id ghế đã chọn, rỗng nếu không chọn được */
    List<Long> choose(SeatLayout layout, int n);

    List<Strategy> ALL = List.of(new Engine(), new GreedyLeftToRight(), new NearestCenter());

    /** Thuật toán đề xuất (phương án điểm cao nhất, tùy chọn mặc định: chia tối đa 3 hàng). */
    final class Engine implements Strategy {
        private final SeatSuggester suggester = new SeatSuggester();

        @Override
        public String name() {
            return "engine";
        }

        @Override
        public List<Long> choose(SeatLayout layout, int n) {
            SuggestionResult result = suggester.suggest(layout, SuggestionRequest.of(n).withTopK(1));
            return result.found() ? result.suggestions().get(0).seatIds() : List.of();
        }
    }

    /**
     * Baseline 1 — tham lam trái → phải: duyệt từ hàng đầu, lấy dãy n ghế trống liền nhau đầu tiên gặp; không có
     * thì lấy n ghế trống đầu tiên theo thứ tự duyệt.
     */
    final class GreedyLeftToRight implements Strategy {
        @Override
        public String name() {
            return "greedy-left-to-right";
        }

        @Override
        public List<Long> choose(SeatLayout layout, int n) {
            Map<Integer, List<SeatSpec>> rows = rows(layout);
            for (List<SeatSpec> row : rows.values()) {
                List<Long> run = new ArrayList<>();
                SeatSpec previous = null;
                for (SeatSpec seat : row) {
                    boolean adjacent = previous != null && seat.gridCol() == previous.gridCol() + previous.colSpan();
                    if (!seat.available() || !adjacent) {
                        run.clear();
                    }
                    if (seat.available()) {
                        run.add(seat.id());
                        if (run.size() == n) {
                            return run;
                        }
                    }
                    previous = seat;
                }
            }
            List<Long> any = new ArrayList<>();
            for (List<SeatSpec> row : rows.values()) {
                for (SeatSpec seat : row) {
                    if (seat.available() && any.size() < n) {
                        any.add(seat.id());
                    }
                }
            }
            return any.size() == n ? any : List.of();
        }
    }

    /** Baseline 2 — n ghế trống bất kỳ gần điểm xem đẹp nhất (không quan tâm liền kề). */
    final class NearestCenter implements Strategy {
        @Override
        public String name() {
            return "nearest-center";
        }

        @Override
        public List<Long> choose(SeatLayout layout, int n) {
            double sweetX = layout.gridCols() / 2.0;
            double sweetY = layout.gridRows() * 2.0 / 3.0;
            List<SeatSpec> free = new ArrayList<>(
                    layout.seats().stream().filter(SeatSpec::available).toList());
            if (free.size() < n) {
                return List.of();
            }
            free.sort(Comparator.comparingDouble((SeatSpec s) -> {
                        double dx = (s.gridCol() + s.colSpan() / 2.0 - sweetX) / sweetX;
                        double dy = (s.gridRow() + 0.5 - sweetY) / layout.gridRows();
                        return dx * dx + dy * dy;
                    })
                    .thenComparingLong(SeatSpec::id));
            return free.subList(0, n).stream().map(SeatSpec::id).toList();
        }
    }

    private static Map<Integer, List<SeatSpec>> rows(SeatLayout layout) {
        Map<Integer, List<SeatSpec>> rows = new TreeMap<>();
        layout.seats().forEach(s -> rows.computeIfAbsent(s.gridRow(), r -> new ArrayList<>()).add(s));
        rows.values().forEach(r -> r.sort(Comparator.comparingInt(SeatSpec::gridCol)));
        return rows;
    }
}
