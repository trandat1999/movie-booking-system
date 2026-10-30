package com.moviebooking.seatbench;

import com.moviebooking.seatbench.RoomGenerator.FillPattern;
import com.moviebooking.seatbench.RoomGenerator.RoomSize;
import com.moviebooking.seatengine.SeatLayout;
import com.moviebooking.seatengine.SeatSuggester;
import com.moviebooking.seatengine.SuggestionRequest;
import com.moviebooking.seatengine.SuggestionResult;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * Thời gian chạy của {@link SeatSuggester#suggest} (PLAN.md 12: p95 &lt; 50 ms với phòng 300 ghế). Chế độ
 * SampleTime cho phân vị p50/p95/p99. Mỗi lần gọi dùng một phòng khác trong bộ 32 phòng sinh sẵn.
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class SuggestBenchmark {

    private static final int ROOMS = 32;

    @Param({"S100", "S200", "S300"})
    public RoomSize size;

    @Param({"0.3", "0.6", "0.85"})
    public double fill;

    @Param({"2", "5", "10"})
    public int n;

    private final SeatSuggester suggester = new SeatSuggester();
    private SeatLayout[] layouts;
    private SuggestionRequest request;
    private int next;

    @Setup
    public void setUp() {
        Random random = new Random(2026);
        layouts = new SeatLayout[ROOMS];
        for (int i = 0; i < ROOMS; i++) {
            layouts[i] = RoomGenerator.generate(size, fill, i % 2 == 0 ? FillPattern.RANDOM : FillPattern.CENTER, random);
        }
        request = SuggestionRequest.of(n);
    }

    @Benchmark
    public SuggestionResult suggest() {
        SeatLayout layout = layouts[next++ & (ROOMS - 1)];
        return suggester.suggest(layout, request);
    }
}
