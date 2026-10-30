package com.moviebooking.devdata;

import com.moviebooking.common.util.TimeZones;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.modules.cinema.dto.request.CinemaRequest;
import com.moviebooking.modules.cinema.dto.request.RoomLayoutRequest;
import com.moviebooking.modules.cinema.dto.request.RoomRequest;
import com.moviebooking.modules.cinema.dto.response.CinemaResponse;
import com.moviebooking.modules.cinema.dto.response.RoomResponse;
import com.moviebooking.modules.cinema.entity.SeatType;
import com.moviebooking.modules.cinema.enums.CinemaStatus;
import com.moviebooking.modules.cinema.enums.RoomStatus;
import com.moviebooking.modules.cinema.enums.RoomType;
import com.moviebooking.modules.cinema.service.CinemaService;
import com.moviebooking.modules.cinema.service.RoomService;
import com.moviebooking.modules.movie.dto.request.GenreRequest;
import com.moviebooking.modules.movie.dto.request.MovieRequest;
import com.moviebooking.modules.movie.dto.request.PersonRequest;
import com.moviebooking.modules.movie.dto.response.MovieDetailResponse;
import com.moviebooking.modules.movie.enums.AgeRating;
import com.moviebooking.modules.movie.enums.MovieStatus;
import com.moviebooking.modules.movie.service.GenreService;
import com.moviebooking.modules.movie.service.MovieService;
import com.moviebooking.modules.movie.service.PersonService;
import com.moviebooking.modules.showtime.dto.request.HolidayRequest;
import com.moviebooking.modules.showtime.dto.request.ShowtimeRequest;
import com.moviebooking.modules.showtime.enums.AudioType;
import com.moviebooking.modules.showtime.enums.ScreenFormat;
import com.moviebooking.modules.showtime.service.PricingService;
import com.moviebooking.modules.showtime.service.ShowtimeService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Dữ liệu demo cho môi trường dev (PLAN.md M2): 3 rạp, 8 phòng (có lối đi, ghế VIP, ghế đôi, ghế khuyết tật),
 * 15 phim và lịch chiếu {@value #SCHEDULE_DAYS} ngày tới. Đi qua service thật nên mọi quy tắc nghiệp vụ được áp
 * dụng. Chỉ chạy khi chưa có rạp nào.
 */
@Slf4j
@Profile("dev")
@Component
@Order(100)
@RequiredArgsConstructor
public class DevCatalogSeeder implements ApplicationRunner {

    static final int SCHEDULE_DAYS = 7;
    private static final LocalTime FIRST_SHOW = LocalTime.of(9, 0);
    private static final LocalTime LAST_START = LocalTime.of(22, 30);

    private final GenreService genreService;
    private final PersonService personService;
    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final RoomService roomService;
    private final PricingService pricingService;
    private final ShowtimeService showtimeService;
    private final Clock clock;

    record RoomPlan(String name, RoomType type, int rows, int seatsPerRow, Set<Integer> aisleAfter, int vipFrom,
            int vipTo, boolean coupleRow) {}

    @Override
    public void run(ApplicationArguments args) {
        if (!cinemaService.list().isEmpty()) {
            return;
        }
        log.info("Seeding demo catalog...");
        Map<String, Long> genres = seedGenres();
        List<Long> people = seedPeople();
        List<MovieDetailResponse> movies = seedMovies(genres, people);
        Map<RoomResponse, RoomPlan> rooms = seedCinemas();
        seedHolidays();
        int created = seedShowtimes(movies, rooms);
        log.info("Seeded {} movies, {} rooms, {} showtimes", movies.size(), rooms.size(), created);
    }

    private Map<String, Long> seedGenres() {
        Map<String, Long> ids = new HashMap<>();
        for (String name : List.of(
                "Hành động", "Hài", "Tình cảm", "Kinh dị", "Hoạt hình", "Khoa học viễn tưởng", "Tâm lý", "Gia đình",
                "Phiêu lưu")) {
            ids.put(name, genreService.create(new GenreRequest(name)).id());
        }
        return ids;
    }

    private List<Long> seedPeople() {
        List<Long> ids = new ArrayList<>();
        for (String name : List.of(
                "Trần Minh Khang", "Lê Thu Hà", "Phạm Quốc Bảo", "Nguyễn Hoài An", "Đỗ Thanh Tâm", "Vũ Gia Huy",
                "Hoàng Mai Linh", "Bùi Đức Long", "Đặng Ngọc Ánh", "Phan Tuấn Kiệt", "Lý Bảo Châu", "Mai Anh Tuấn")) {
            ids.add(personService.create(new PersonRequest(name, null)).id());
        }
        return ids;
    }

    private List<MovieDetailResponse> seedMovies(Map<String, Long> g, List<Long> p) {
        LocalDate today = LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        Object[][] data = {
            // tiêu đề, phút, độ tuổi, thể loại, trạng thái, lệch ngày khởi chiếu
            {"Bầu Trời Thép", 128, AgeRating.T13, List.of("Hành động", "Khoa học viễn tưởng"), MovieStatus.NOW_SHOWING, -20},
            {"Mùa Hè Ở Hội An", 104, AgeRating.P, List.of("Tình cảm", "Gia đình"), MovieStatus.NOW_SHOWING, -14},
            {"Ngôi Nhà Số 13", 97, AgeRating.T18, List.of("Kinh dị"), MovieStatus.NOW_SHOWING, -10},
            {"Biệt Đội Mèo Mun", 92, AgeRating.P, List.of("Hoạt hình", "Hài", "Gia đình"), MovieStatus.NOW_SHOWING, -7},
            {"Đường Đua Sấm Sét", 135, AgeRating.T16, List.of("Hành động", "Phiêu lưu"), MovieStatus.NOW_SHOWING, -5},
            {"Bí Mật Phố Cổ", 112, AgeRating.T13, List.of("Tâm lý", "Tình cảm"), MovieStatus.NOW_SHOWING, -3},
            {"Hành Tinh Lặng Im", 141, AgeRating.T13, List.of("Khoa học viễn tưởng", "Phiêu lưu"), MovieStatus.NOW_SHOWING, -2},
            {"Ông Ngoại Siêu Quậy", 99, AgeRating.K, List.of("Hài", "Gia đình"), MovieStatus.NOW_SHOWING, -1},
            {"Tiếng Gọi Đại Ngàn", 118, AgeRating.T13, List.of("Phiêu lưu"), MovieStatus.NOW_SHOWING, -30},
            {"Đêm Trăng Máu", 101, AgeRating.T18, List.of("Kinh dị", "Tâm lý"), MovieStatus.NOW_SHOWING, -12},
            {"Hẹn Em Ngày Nắng", 109, AgeRating.T13, List.of("Tình cảm"), MovieStatus.COMING_SOON, 10},
            {"Rồng Đá Thức Giấc", 125, AgeRating.K, List.of("Hoạt hình", "Phiêu lưu"), MovieStatus.COMING_SOON, 14},
            {"Kẻ Săn Bóng Đêm", 132, AgeRating.T16, List.of("Hành động"), MovieStatus.COMING_SOON, 21},
            {"Lớp Học Cuối Cùng", 95, AgeRating.T13, List.of("Tâm lý", "Gia đình"), MovieStatus.COMING_SOON, 28},
            {"Chuyến Tàu Năm Ấy", 116, AgeRating.T13, List.of("Tình cảm", "Tâm lý"), MovieStatus.ENDED, -90},
        };
        List<MovieDetailResponse> movies = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            Object[] m = data[i];
            @SuppressWarnings("unchecked")
            List<String> genreNames = (List<String>) m[3];
            movies.add(movieService.create(new MovieRequest(
                    (String) m[0],
                    null,
                    "Phim demo cho đồ án — nội dung mô tả ngắn của bộ phim \"" + m[0] + "\".",
                    (Integer) m[1],
                    (AgeRating) m[2],
                    i % 3 == 0 ? "Tiếng Anh" : "Tiếng Việt",
                    today.plusDays((Integer) m[5]),
                    null,
                    null,
                    null,
                    (MovieStatus) m[4],
                    Set.copyOf(genreNames.stream().map(g::get).toList()),
                    List.of(p.get(i % 4)),
                    List.of(p.get(4 + i % 8), p.get(4 + (i + 3) % 8)))));
        }
        return movies;
    }

    private Map<RoomResponse, RoomPlan> seedCinemas() {
        Map<RoomResponse, RoomPlan> rooms = new java.util.LinkedHashMap<>();
        CinemaResponse hcm = cinemaService.create(new CinemaRequest(
                "MovieBooking Quận 1", "72 Lê Thánh Tôn, Phường Bến Nghé, Quận 1", "TP. Hồ Chí Minh", "02873000001",
                "Rạp trung tâm với phòng IMAX.", null,
                List.of("Bãi đỗ xe", "Quầy bắp nước", "Phòng chờ VIP", "Thang máy"), CinemaStatus.ACTIVE));
        CinemaResponse hn = cinemaService.create(new CinemaRequest(
                "MovieBooking Cầu Giấy", "241 Xuân Thủy, Cầu Giấy", "Hà Nội", "02473000002", null, null,
                List.of("Bãi đỗ xe", "Quầy bắp nước"), CinemaStatus.ACTIVE));
        CinemaResponse dn = cinemaService.create(new CinemaRequest(
                "MovieBooking Hải Châu", "255 Hùng Vương, Hải Châu", "Đà Nẵng", "02363000003", null, null,
                List.of("Quầy bắp nước"), CinemaStatus.ACTIVE));

        addRoom(rooms, hcm.id(), new RoomPlan("Phòng 1", RoomType.STANDARD, 10, 14, Set.of(3, 11), 4, 7, true));
        addRoom(rooms, hcm.id(), new RoomPlan("Phòng 2", RoomType.STANDARD, 8, 12, Set.of(6), 3, 5, false));
        addRoom(rooms, hcm.id(), new RoomPlan("Phòng 3", RoomType.PREMIUM, 7, 10, Set.of(2, 8), 2, 5, true));
        addRoom(rooms, hcm.id(), new RoomPlan("IMAX", RoomType.IMAX, 12, 20, Set.of(4, 16), 5, 9, true));
        addRoom(rooms, hn.id(), new RoomPlan("Phòng 1", RoomType.STANDARD, 9, 14, Set.of(3, 11), 4, 6, true));
        addRoom(rooms, hn.id(), new RoomPlan("Phòng 2", RoomType.PREMIUM, 7, 10, Set.of(5), 2, 4, true));
        addRoom(rooms, dn.id(), new RoomPlan("Phòng 1", RoomType.STANDARD, 8, 12, Set.of(2, 10), 3, 5, true));
        addRoom(rooms, dn.id(), new RoomPlan("Phòng 2", RoomType.STANDARD, 6, 10, Set.of(5), 2, 3, false));
        return rooms;
    }

    private void addRoom(Map<RoomResponse, RoomPlan> rooms, Long cinemaId, RoomPlan plan) {
        RoomResponse room = roomService.create(cinemaId, new RoomRequest(plan.name(), plan.type(), RoomStatus.ACTIVE));
        roomService.saveLayout(room.id(), layout(plan));
        rooms.put(room, plan);
    }

    /**
     * Lưới: hàng 0 gần màn hình; lối đi dọc sau các ghế thứ {@code aisleAfter}; hàng VIP ở giữa phòng; hàng cuối
     * là ghế đôi; hai ghế ngoài cùng hàng đầu dành cho người khuyết tật.
     */
    static RoomLayoutRequest layout(RoomPlan plan) {
        int gridCols = plan.seatsPerRow() + plan.aisleAfter().size();
        List<RoomLayoutRequest.SeatCell> cells = new ArrayList<>();
        for (int row = 0; row < plan.rows(); row++) {
            boolean coupleRow = plan.coupleRow() && row == plan.rows() - 1;
            List<int[]> blocks = new ArrayList<>();
            int col = 0;
            int blockStart = 0;
            for (int seat = 1; seat <= plan.seatsPerRow(); seat++) {
                col++;
                if (plan.aisleAfter().contains(seat) || seat == plan.seatsPerRow()) {
                    blocks.add(new int[] {blockStart, col});
                    col++;
                    blockStart = col;
                }
            }
            for (int[] block : blocks) {
                int c = block[0];
                while (c < block[1]) {
                    if (coupleRow && c + 1 < block[1]) {
                        cells.add(new RoomLayoutRequest.SeatCell(row, c, SeatType.COUPLE));
                        c += 2;
                        continue;
                    }
                    String type = SeatType.STANDARD;
                    if (row == 0 && (c == 0 || c == gridCols - 1)) {
                        type = SeatType.ACCESSIBLE;
                    } else if (row >= plan.vipFrom() && row <= plan.vipTo()) {
                        type = SeatType.VIP;
                    }
                    cells.add(new RoomLayoutRequest.SeatCell(row, c, type));
                    c++;
                }
            }
        }
        return new RoomLayoutRequest(plan.rows(), gridCols, cells);
    }

    private void seedHolidays() {
        LocalDate today = LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        for (int year = today.getYear(); year <= today.getYear() + 1; year++) {
            addHoliday(LocalDate.of(year, 1, 1), "Tết Dương lịch");
            addHoliday(LocalDate.of(year, 4, 30), "Ngày Giải phóng miền Nam");
            addHoliday(LocalDate.of(year, 5, 1), "Quốc tế Lao động");
            addHoliday(LocalDate.of(year, 9, 2), "Quốc khánh");
        }
    }

    private void addHoliday(LocalDate date, String name) {
        try {
            pricingService.addHoliday(new HolidayRequest(date, name));
        } catch (BusinessException ignored) {
            // đã có
        }
    }

    /** Mỗi phòng mỗi ngày chiếu liên tục từ 9:00, các phim đang chiếu xoay vòng. */
    private int seedShowtimes(List<MovieDetailResponse> movies, Map<RoomResponse, RoomPlan> rooms) {
        List<MovieDetailResponse> showing = movies.stream()
                .filter(m -> m.status() == MovieStatus.NOW_SHOWING)
                .toList();
        Instant earliest = clock.instant().plusSeconds(30 * 60);
        LocalDate today = LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        int created = 0;
        int movieIndex = 0;
        for (RoomResponse room : rooms.keySet()) {
            for (int day = 0; day < SCHEDULE_DAYS; day++) {
                ZonedDateTime start = today.plusDays(day).atTime(FIRST_SHOW).atZone(TimeZones.VIETNAM);
                while (!start.toLocalTime().isAfter(LAST_START) && start.toLocalDate().equals(today.plusDays(day))) {
                    MovieDetailResponse movie = showing.get(movieIndex++ % showing.size());
                    if (start.toInstant().isAfter(earliest)) {
                        showtimeService.create(new ShowtimeRequest(
                                movie.id(), room.id(), start.toInstant(), formatFor(room, movieIndex),
                                movie.genres().stream().anyMatch(g -> g.name().equals("Hoạt hình"))
                                        ? AudioType.DUBBED
                                        : AudioType.SUBTITLED,
                                null));
                        created++;
                    }
                    // giờ kết thúc + 15 phút dọn phòng, làm tròn lên 15 phút
                    ZonedDateTime next = start.plusMinutes(movie.durationMin() + 15L);
                    int roundUp = (15 - next.getMinute() % 15) % 15;
                    start = next.plusMinutes(roundUp).withSecond(0);
                }
            }
        }
        return created;
    }

    private static ScreenFormat formatFor(RoomResponse room, int index) {
        if (room.roomType() == RoomType.IMAX) {
            return ScreenFormat.IMAX;
        }
        return room.roomType() == RoomType.PREMIUM && index % 2 == 0 ? ScreenFormat.THREE_D : ScreenFormat.TWO_D;
    }
}
