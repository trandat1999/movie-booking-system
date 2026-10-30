package com.moviebooking.seatbench;

import com.moviebooking.seatbench.RoomGenerator.FillPattern;
import com.moviebooking.seatbench.RoomGenerator.RoomSize;
import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSpec;
import com.moviebooking.seatengine.SeatSuggester;
import com.moviebooking.seatengine.Suggestion;
import com.moviebooking.seatengine.SuggestionRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Thực nghiệm so sánh thuật toán với 2 baseline (PLAN.md 8.2): mọi tổ hợp kích thước phòng × tỷ lệ lấp đầy × kiểu
 * lấp đầy × n = 2..10, mỗi tổ hợp {@code trials} phòng ngẫu nhiên (seed cố định → tái lập được). Mọi phương án
 * được chấm bằng cùng hàm điểm của engine ({@link SeatSuggester#evaluate}).
 *
 * <p>Chạy: {@code java -cp <classpath> com.moviebooking.seatbench.Experiment <thư mục kết quả> [trials]}.
 */
public final class Experiment {

    static final double[] FILL_RATES = {0.3, 0.6, 0.85};
    static final int MIN_N = 2;
    static final int MAX_N = 10;

    private final SeatSuggester suggester = new SeatSuggester();

    /** Kết quả một chiến lược trên một tổ hợp cấu hình. */
    record Row(RoomSize size, double fill, FillPattern pattern, int n, String strategy, Stats stats) {}

    static final class Stats {
        int trials;
        int found;
        int sameRow;
        double scoreSum;
        int orphanSum;
        int fragmentSum;
        /** Các lần thử mà engine tìm được cụm — so sánh điểm trên cùng tập phòng. */
        int common;
        double commonScoreSum;
        final List<Long> nanos = new ArrayList<>();

        void merge(Stats other) {
            trials += other.trials;
            found += other.found;
            sameRow += other.sameRow;
            scoreSum += other.scoreSum;
            common += other.common;
            commonScoreSum += other.commonScoreSum;
            orphanSum += other.orphanSum;
            fragmentSum += other.fragmentSum;
            nanos.addAll(other.nanos);
        }

        double foundRate() {
            return (double) found / trials;
        }

        double sameRowRate() {
            return (double) sameRow / trials;
        }

        double avgScore() {
            return found == 0 ? 0 : scoreSum / found;
        }

        double avgScoreWhenEngineFound() {
            return common == 0 ? 0 : commonScoreSum / common;
        }

        double avgOrphans() {
            return found == 0 ? 0 : (double) orphanSum / found;
        }

        double avgFragments() {
            return found == 0 ? 0 : (double) fragmentSum / found;
        }

        double percentileMs(double p) {
            List<Long> sorted = new ArrayList<>(nanos);
            sorted.sort(null);
            int index = (int) Math.ceil(p * sorted.size()) - 1;
            return sorted.get(Math.max(0, index)) / 1_000_000.0;
        }
    }

    public static void main(String[] args) throws IOException {
        Path out = Path.of(args.length > 0 ? args[0] : "docs/experiments");
        int trials = args.length > 1 ? Integer.parseInt(args[1]) : 30;
        Files.createDirectories(out);
        Experiment experiment = new Experiment();
        experiment.warmUp();
        List<Row> rows = experiment.run(trials);
        experiment.write(out, rows, trials);
        System.out.println("Wrote " + rows.size() + " result rows to " + out.toAbsolutePath());
    }

    /** Chạy trước để JIT biên dịch, tránh làm lệch số đo thời gian. */
    void warmUp() {
        Random random = new Random(1);
        for (int i = 0; i < 300; i++) {
            SeatLayout layout = RoomGenerator.generate(RoomSize.S300, 0.6, FillPattern.RANDOM, random);
            for (Strategy strategy : Strategy.ALL) {
                strategy.choose(layout, 2 + i % 9);
            }
        }
    }

    List<Row> run(int trials) {
        List<Row> rows = new ArrayList<>();
        for (RoomSize size : RoomSize.values()) {
            for (double fill : FILL_RATES) {
                for (FillPattern pattern : FillPattern.values()) {
                    for (int n = MIN_N; n <= MAX_N; n++) {
                        Map<String, Stats> byStrategy = new LinkedHashMap<>();
                        Strategy.ALL.forEach(s -> byStrategy.put(s.name(), new Stats()));
                        for (int t = 0; t < trials; t++) {
                            long seed = ((size.ordinal() * 1000L + Math.round(fill * 100)) * 10 + pattern.ordinal())
                                    * 100_000 + n * 1000L + t;
                            SeatLayout layout = RoomGenerator.generate(size, fill, pattern, new Random(seed));
                            // Strategy.ALL bắt đầu bằng engine → biết engine có tìm được cụm trước khi đo baseline
                            Boolean engineFound = null;
                            for (Strategy strategy : Strategy.ALL) {
                                Suggestion evaluated = measure(strategy, layout, n, byStrategy.get(strategy.name()));
                                if (engineFound == null) {
                                    engineFound = evaluated != null;
                                }
                                if (engineFound && evaluated != null) {
                                    Stats stats = byStrategy.get(strategy.name());
                                    stats.common++;
                                    stats.commonScoreSum += evaluated.score();
                                }
                            }
                        }
                        int finalN = n;
                        byStrategy.forEach((name, stats) -> rows.add(new Row(size, fill, pattern, finalN, name, stats)));
                    }
                }
            }
        }
        return rows;
    }

    /** @return phương án đã chấm điểm, null nếu chiến lược không chọn được */
    private Suggestion measure(Strategy strategy, SeatLayout layout, int n, Stats stats) {
        long start = System.nanoTime();
        List<Long> chosen = strategy.choose(layout, n);
        stats.nanos.add(System.nanoTime() - start);
        stats.trials++;
        if (chosen.isEmpty()) {
            return null;
        }
        Suggestion evaluated = suggester.evaluate(layout, SuggestionRequest.of(n), chosen);
        int fragments = fragments(layout, chosen);
        stats.found++;
        stats.scoreSum += evaluated.score();
        stats.orphanSum += evaluated.breakdown().orphanSeats();
        stats.fragmentSum += fragments;
        if (fragments == 1) {
            stats.sameRow++;
        }
        return evaluated;
    }

    /** Số cụm ghế liền kề của lựa chọn (1 = cùng hàng, liền nhau). */
    static int fragments(SeatLayout layout, List<Long> chosen) {
        Map<Long, SeatSpec> byId = layout.seats().stream().collect(Collectors.toMap(SeatSpec::id, Function.identity()));
        List<SeatSpec> seats = chosen.stream()
                .map(byId::get)
                .sorted(Comparator.comparingInt(SeatSpec::gridRow).thenComparingInt(SeatSpec::gridCol))
                .toList();
        int fragments = 1;
        for (int i = 1; i < seats.size(); i++) {
            SeatSpec prev = seats.get(i - 1);
            SeatSpec cur = seats.get(i);
            if (cur.gridRow() != prev.gridRow() || cur.gridCol() != prev.gridCol() + prev.colSpan()) {
                fragments++;
            }
        }
        return fragments;
    }

    void write(Path out, List<Row> rows, int trials) throws IOException {
        StringBuilder detail = new StringBuilder(
                "room_seats,fill_rate,fill_pattern,n,strategy,trials,found_rate,same_row_rate,avg_score,avg_score_common,"
                        + "avg_orphans,avg_fragments,p50_ms,p95_ms\n");
        for (Row r : rows) {
            detail.append(csv(r.size().seats(), r.fill(), r.pattern(), r.n(), r.strategy(), r.stats()));
        }
        Files.writeString(out.resolve("seat-suggestion-detail.csv"), detail, StandardCharsets.UTF_8);

        // Tổng hợp theo tỷ lệ lấp đầy × kiểu lấp đầy × chiến lược (gộp mọi kích thước phòng và n)
        Map<String, Stats> summary = new LinkedHashMap<>();
        for (Row r : rows) {
            summary.computeIfAbsent(r.fill() + "," + r.pattern() + "," + r.strategy(), k -> new Stats())
                    .merge(r.stats());
        }
        StringBuilder summaryCsv = new StringBuilder(
                "fill_rate,fill_pattern,strategy,trials,found_rate,same_row_rate,avg_score,avg_score_common,avg_orphans,"
                        + "avg_fragments,p50_ms,p95_ms\n");
        summary.forEach((key, s) -> summaryCsv.append(key).append(',').append(metrics(s)).append('\n'));
        Files.writeString(out.resolve("seat-suggestion-summary.csv"), summaryCsv, StandardCharsets.UTF_8);

        // Thời gian chạy của engine ở phòng 300 ghế theo n
        StringBuilder timing = new StringBuilder("n,runs,p50_ms,p95_ms,max_ms\n");
        for (int n = MIN_N; n <= MAX_N; n++) {
            Stats s = new Stats();
            for (Row r : rows) {
                if (r.size() == RoomSize.S300 && r.n() == n && r.strategy().equals("engine")) {
                    s.merge(r.stats());
                }
            }
            timing.append(String.format(
                    Locale.ROOT, "%d,%d,%.3f,%.3f,%.3f%n", n, s.nanos.size(), s.percentileMs(0.5),
                    s.percentileMs(0.95), s.percentileMs(1.0)));
        }
        Files.writeString(out.resolve("engine-timing-300-seats.csv"), timing, StandardCharsets.UTF_8);

        writeCharts(out, rows);
        System.out.println("Trials per configuration: " + trials);
    }

    private void writeCharts(Path out, List<Row> rows) throws IOException {
        List<String> strategies = Strategy.ALL.stream().map(Strategy::name).toList();
        List<String> categories = new ArrayList<>();
        for (double fill : FILL_RATES) {
            categories.add(String.format(Locale.ROOT, "Lấp đầy %.0f%%", fill * 100));
        }
        double[][] sameRow = new double[strategies.size()][FILL_RATES.length];
        double[][] orphans = new double[strategies.size()][FILL_RATES.length];
        double[][] score = new double[strategies.size()][FILL_RATES.length];
        for (int s = 0; s < strategies.size(); s++) {
            for (int f = 0; f < FILL_RATES.length; f++) {
                Stats agg = new Stats();
                for (Row r : rows) {
                    if (r.strategy().equals(strategies.get(s)) && r.fill() == FILL_RATES[f]) {
                        agg.merge(r.stats());
                    }
                }
                sameRow[s][f] = agg.sameRowRate() * 100;
                orphans[s][f] = agg.avgOrphans();
                score[s][f] = agg.avgScoreWhenEngineFound();
            }
        }
        Files.writeString(
                out.resolve("same-row-rate.svg"),
                SvgBarChart.render("Tỷ lệ tìm được cụm liền nhau cùng hàng (%)", categories, strategies, sameRow, 100, "%.0f"),
                StandardCharsets.UTF_8);
        Files.writeString(
                out.resolve("avg-orphans.svg"),
                SvgBarChart.render("Số ghế lẻ tạo ra trung bình", categories, strategies, orphans, 2, "%.2f"),
                StandardCharsets.UTF_8);
        Files.writeString(
                out.resolve("avg-score.svg"),
                SvgBarChart.render("Điểm trung bình trên các phòng engine tìm được cụm (0–100)", categories, strategies, score, 100, "%.0f"),
                StandardCharsets.UTF_8);
    }

    private static String csv(int seats, double fill, FillPattern pattern, int n, String strategy, Stats s) {
        return String.format(Locale.ROOT, "%d,%.2f,%s,%d,%s,%s%n", seats, fill, pattern, n, strategy, metrics(s));
    }

    private static String metrics(Stats s) {
        return String.format(
                Locale.ROOT,
                "%d,%.3f,%.3f,%.2f,%.2f,%.3f,%.3f,%.3f,%.3f",
                s.trials, s.foundRate(), s.sameRowRate(), s.avgScore(), s.avgScoreWhenEngineFound(), s.avgOrphans(),
                s.avgFragments(),
                s.percentileMs(0.5), s.percentileMs(0.95));
    }
}
