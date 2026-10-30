package com.moviebooking.modules.stats.service.impl;

import com.moviebooking.common.util.TimeZones;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.stats.dto.response.GroupStats;
import com.moviebooking.modules.stats.dto.response.OverviewStats;
import com.moviebooking.modules.stats.repository.StatsRepository;
import com.moviebooking.modules.stats.repository.StatsRepository.Filter;
import com.moviebooking.modules.stats.service.StatsService;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 366;
    private static final DateTimeFormatter CSV_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(TimeZones.VIETNAM);

    private final StatsRepository statsRepository;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public OverviewStats overview(LocalDate from, LocalDate to, Long cinemaId, Long movieId) {
        LocalDate[] range = range(from, to);
        Filter f = filter(range, cinemaId, movieId);
        long[] sales = statsRepository.sales(f);
        long[] refunds = statsRepository.refunds(f);
        long[] payments = statsRepository.payments(f);
        long[] seats = statsRepository.occupancy(f);
        return new OverviewStats(
                range[0],
                range[1],
                sales[1],
                sales[0],
                sales[3],
                sales[0] == 0 ? 0 : sales[1] / sales[0],
                sales[2],
                refunds[1],
                refunds[0],
                payments[0],
                payments[1],
                ratio(payments[1], payments[0]),
                statsRepository.expiredOrders(f),
                seats[0],
                seats[1],
                ratio(seats[0], seats[1]),
                statsRepository.daily(f),
                statsRepository.byMovie(f, 5),
                statsRepository.byCinema(f));
    }

    @Override
    @Transactional(readOnly = true)
    public GroupStats groups(LocalDate from, LocalDate to, Long movieId) {
        LocalDate[] range = range(from, to);
        Filter f = filter(range, null, movieId);
        Map<String, Long> byStatus = statsRepository.groupsByStatus(f);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();
        long booked = byStatus.getOrDefault("BOOKED", 0L) + byStatus.getOrDefault("COMPLETED", 0L);
        Double[] averages = statsRepository.groupAverages(f);
        GroupStats.SuggestionStats suggestions = statsRepository.suggestions(f);
        List<GroupStats.Bucket> failures = new ArrayList<>();
        failures.add(new GroupStats.Bucket("Quá hạn chốt", byStatus.getOrDefault("EXPIRED", 0L)));
        failures.add(new GroupStats.Bucket("Trưởng nhóm hủy", byStatus.getOrDefault("CANCELLED", 0L)));
        failures.add(new GroupStats.Bucket("Không còn cụm ghế phù hợp", suggestions.noCluster()));
        failures.add(new GroupStats.Bucket("Hết hạn giữ ghế", statsRepository.groupHoldsExpired(f)));
        return new GroupStats(
                range[0],
                range[1],
                total,
                byStatus,
                booked,
                ratio(booked, total),
                averages[0] == null ? 0 : averages[0],
                averages[1] == null ? 0 : averages[1],
                averages[2],
                suggestions,
                statsRepository.sizeBuckets(f),
                failures);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportOrdersCsv(LocalDate from, LocalDate to, Long cinemaId, Long movieId) {
        List<StatsRepository.OrderExportRow> rows = statsRepository.exportOrders(filter(range(from, to), cinemaId, movieId));
        StringBuilder csv = new StringBuilder(
                "Mã đơn,Ngày tạo,Ngày thanh toán,Trạng thái,Email,Khách hàng,Phim,Rạp,Suất chiếu,Số vé,Tạm tính,Giảm giá,"
                        + "Tổng tiền,Mã giảm giá,Đơn nhóm\r\n");
        for (StatsRepository.OrderExportRow r : rows) {
            csv.append(String.join(
                            ",",
                            r.code(),
                            CSV_TIME.format(r.createdAt()),
                            r.paidAt() == null ? "" : CSV_TIME.format(r.paidAt()),
                            r.status(),
                            escape(r.customerEmail()),
                            escape(r.customerName()),
                            escape(r.movie()),
                            escape(r.cinema()),
                            CSV_TIME.format(r.startAt()),
                            String.valueOf(r.tickets()),
                            String.valueOf(r.subtotal()),
                            String.valueOf(r.discount()),
                            String.valueOf(r.total()),
                            r.promotionCode() == null ? "" : r.promotionCode(),
                            r.group() ? "Có" : ""))
                    .append("\r\n");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        out.writeBytes(csv.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    /** [from, to] theo ngày VN; mặc định 30 ngày tới hôm nay. */
    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_DAYS - 1L);
        if (start.isAfter(end) || start.plusDays(MAX_DAYS).isBefore(end)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Khoảng ngày không hợp lệ (tối đa " + MAX_DAYS + " ngày)");
        }
        return new LocalDate[] {start, end};
    }

    private static Filter filter(LocalDate[] range, Long cinemaId, Long movieId) {
        Instant from = range[0].atStartOfDay(TimeZones.VIETNAM).toInstant();
        Instant to = range[1].plusDays(1).atStartOfDay(TimeZones.VIETNAM).toInstant();
        return new Filter(from, to, cinemaId, movieId);
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        // Chặn chèn công thức khi mở bằng Excel (CSV injection)
        String safe = value.matches("^[=+\\-@].*") ? "'" + value : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }

    private static double ratio(long part, long whole) {
        return whole == 0 ? 0 : (double) part / whole;
    }
}
