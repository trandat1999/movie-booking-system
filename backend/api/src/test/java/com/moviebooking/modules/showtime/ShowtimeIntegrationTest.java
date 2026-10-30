package com.moviebooking.modules.showtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.common.util.TimeZones;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ShowtimeIntegrationTest extends AbstractAdminIntegrationTest {

    @Test
    void createComputesEndTimeSeatsAndWeekdayPrices() throws Exception {
        long movieId = createMovie(120);
        long roomId = createRoomWithLayout();
        Instant start = at(DayOfWeek.WEDNESDAY, 10, 0);

        createShowtime(movieId, roomId, start)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.endAt").value(start.plus(Duration.ofMinutes(120)).toString()))
                .andExpect(jsonPath("$.data.blockedUntil").value(start.plus(Duration.ofMinutes(135)).toString()))
                .andExpect(jsonPath("$.data.dayType").value("WEEKDAY"))
                .andExpect(jsonPath("$.data.phase").value("UPCOMING"))
                .andExpect(jsonPath("$.data.seats.total").value(6))
                .andExpect(jsonPath("$.data.seats.available").value(6))
                // giá = cơ bản 75.000 × số chỗ + phụ thu
                .andExpect(jsonPath("$.data.prices[0].seatTypeCode").value("STANDARD"))
                .andExpect(jsonPath("$.data.prices[0].price").value(75_000))
                .andExpect(jsonPath("$.data.prices[1].price").value(95_000))
                .andExpect(jsonPath("$.data.prices[2].price").value(180_000));
    }

    @Test
    void weekendAndHolidayUseTheirOwnPrices() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        createShowtime(movieId, roomId, at(DayOfWeek.SATURDAY, 10, 0))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dayType").value("WEEKEND"))
                .andExpect(jsonPath("$.data.prices[0].price").value(90_000));

        LocalDate holiday = LocalDate.now(clock.withZone(TimeZones.VIETNAM))
                .plusDays(200)
                .with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));
        postJson("/api/v1/admin/pricing/holidays", Map.of("date", holiday.toString(), "name", "Ngày lễ test"))
                .andExpect(status().isCreated());
        createShowtime(movieId, roomId, holiday.atTime(10, 0).atZone(TimeZones.VIETNAM).toInstant())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dayType").value("HOLIDAY"))
                .andExpect(jsonPath("$.data.prices[0].price").value(100_000));
    }

    @Test
    void priceOverridesApplyOnlyToSeatTypesInRoom() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        Map<String, Object> body = showtimeBody(movieId, roomId, at(DayOfWeek.THURSDAY, 10, 0));
        body.put("prices", Map.of("VIP", 99_000));
        postJson("/api/v1/admin/showtimes", body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.prices[1].price").value(99_000));

        body.put("startAt", at(DayOfWeek.THURSDAY, 18, 0).toString());
        body.put("prices", Map.of("ACCESSIBLE", 50_000));
        postJson("/api/v1/admin/showtimes", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SEAT_TYPE_NOT_FOUND"));
    }

    @Test
    void rejectsOverlapIncludingCleaningBufferButAllowsBackToBack() throws Exception {
        long movieId = createMovie(100);
        long roomId = createRoomWithLayout();
        Instant first = at(DayOfWeek.FRIDAY, 9, 0);
        long firstId = idOf(createShowtime(movieId, roomId, first).andExpect(status().isCreated()).andReturn());

        // Bắt đầu 10 phút sau khi suất trước kết thúc: vướng 15 phút dọn phòng
        createShowtime(movieId, roomId, first.plus(Duration.ofMinutes(110)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SHOWTIME_OVERLAP"))
                .andExpect(jsonPath("$.error.details[0].id").value(firstId));
        // Bắt đầu đúng lúc dọn phòng xong: hợp lệ
        createShowtime(movieId, roomId, first.plus(Duration.ofMinutes(115))).andExpect(status().isCreated());
        // Phòng khác cùng giờ: hợp lệ
        createShowtime(movieId, createRoomWithLayout(), first).andExpect(status().isCreated());
    }

    @Test
    void cancelledShowtimeFreesTheSlotAndCannotBeEdited() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        Instant start = at(DayOfWeek.TUESDAY, 20, 0);
        long id = idOf(createShowtime(movieId, roomId, start).andReturn());

        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", id).header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.phase").value("CANCELLED"));
        createShowtime(movieId, roomId, start).andExpect(status().isCreated());

        putJson("/api/v1/admin/showtimes/{id}", Map.of("startAt", start.plus(Duration.ofDays(1)).toString(),
                        "format", "TWO_D", "audioType", "DUBBED"), id)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SHOWTIME_NOT_EDITABLE"));
    }

    @Test
    void updateRecalculatesScheduleAndChecksOverlapExcludingItself() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        Instant start = at(DayOfWeek.MONDAY, 14, 0);
        long id = idOf(createShowtime(movieId, roomId, start).andReturn());

        // Dời 30 phút: chồng lên chính nó nhưng không tính là trùng
        Instant moved = start.plus(Duration.ofMinutes(30));
        putJson("/api/v1/admin/showtimes/{id}",
                        Map.of("startAt", moved.toString(), "format", "THREE_D", "audioType", "DUBBED"), id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.endAt").value(moved.plus(Duration.ofMinutes(90)).toString()))
                .andExpect(jsonPath("$.data.prices[0].price").value(95_000));
    }

    @Test
    void validatesMovieRoomAndTime() throws Exception {
        long roomId = createRoomWithLayout();
        long movieId = createMovie(90);

        createShowtime(movieId, roomId, clock.instant().minus(Duration.ofHours(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SHOWTIME_IN_PAST"));

        long ended = idOf(postJson("/api/v1/admin/movies", movieBody(unique("Phim cũ"), 90, "ENDED", List.of(createGenre())))
                .andReturn());
        createShowtime(ended, roomId, at(DayOfWeek.MONDAY, 9, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MOVIE_NOT_SCHEDULABLE"));

        long emptyRoom = createRoom(createCinema());
        createShowtime(movieId, emptyRoom, at(DayOfWeek.MONDAY, 9, 0))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_SCHEDULABLE"));
    }

    @Test
    void showtimeLocksLayoutAndBlocksMovieDeletion() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        long showtimeId = idOf(createShowtime(movieId, roomId, at(DayOfWeek.WEDNESDAY, 21, 0)).andReturn());

        putJson("/api/v1/admin/rooms/{id}/layout", smallLayout(), roomId)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ROOM_LAYOUT_LOCKED"));
        mockMvc.perform(delete("/api/v1/admin/movies/{id}", movieId).header("Authorization", admin.bearer()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("MOVIE_IN_USE"));

        // Tắt bán một ghế → suất chưa chiếu có 1 ghế BLOCKED; bật lại → về AVAILABLE
        String layout = mockMvc.perform(get("/api/v1/admin/rooms/{id}", roomId).header("Authorization", admin.bearer()))
                .andReturn().getResponse().getContentAsString();
        long seatId = ((Number) JsonPath.read(layout, "$.data.seats[0].id")).longValue();
        setSeatActive(roomId, seatId, false);
        mockMvc.perform(get("/api/v1/admin/showtimes/{id}", showtimeId).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.seats.blocked").value(1))
                .andExpect(jsonPath("$.data.seats.available").value(5));
        setSeatActive(roomId, seatId, true);
        mockMvc.perform(get("/api/v1/admin/showtimes/{id}", showtimeId).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.seats.blocked").value(0));
    }

    /** Nhiều quản trị viên tạo cùng một khung giờ cùng lúc: chỉ đúng một yêu cầu thành công. */
    @Test
    void concurrentCreationInSameSlotAllowsExactlyOne() throws Exception {
        long movieId = createMovie(120);
        long roomId = createRoomWithLayout();
        Instant start = at(DayOfWeek.SUNDAY, 15, 0);
        String body = json(showtimeBody(movieId, roomId, start));

        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            // lệch vài phút để mỗi yêu cầu là một khoảng khác nhau nhưng đều chồng lên nhau
            String shifted = body.replace(start.toString(), start.plus(Duration.ofMinutes(i)).toString());
            results.add(pool.submit(() -> {
                ready.await();
                return mockMvc.perform(post("/api/v1/admin/showtimes")
                                .header("Authorization", admin.bearer())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(shifted))
                        .andReturn()
                        .getResponse()
                        .getStatus();
            }));
        }
        ready.countDown();
        List<Integer> statuses = new ArrayList<>();
        for (Future<Integer> result : results) {
            statuses.add(result.get());
        }
        pool.shutdown();

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(1);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(threads - 1);
    }

    @Test
    void staffCannotManageShowtimes() throws Exception {
        Session staff = login(createUser(uniqueEmail("staff"), Role.STAFF).getEmail(), PASSWORD);
        mockMvc.perform(get("/api/v1/admin/showtimes").header("Authorization", staff.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFiltersByLocalDateAndRoom() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        Instant start = at(DayOfWeek.THURSDAY, 23, 30); // 23:30 giờ VN = 16:30 UTC cùng ngày
        createShowtime(movieId, roomId, start).andExpect(status().isCreated());
        LocalDate localDate = start.atZone(TimeZones.VIETNAM).toLocalDate();

        mockMvc.perform(get("/api/v1/admin/showtimes")
                        .param("roomId", String.valueOf(roomId))
                        .param("date", localDate.toString())
                        .header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/admin/showtimes")
                        .param("roomId", String.valueOf(roomId))
                        .param("date", localDate.plusDays(1).toString())
                        .header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    private void setSeatActive(long roomId, long seatId, boolean active) throws Exception {
        mockMvc.perform(patch("/api/v1/admin/rooms/{roomId}/seats/{seatId}", roomId, seatId)
                        .header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", active))))
                .andExpect(status().isOk());
    }

    private Map<String, Object> showtimeBody(long movieId, long roomId, Instant startAt) {
        Map<String, Object> body = new HashMap<>();
        body.put("movieId", movieId);
        body.put("roomId", roomId);
        body.put("startAt", startAt.toString());
        body.put("format", "TWO_D");
        body.put("audioType", "SUBTITLED");
        return body;
    }

    /** Thứ trong tuần kế tiếp (cách hôm nay ít nhất 2 ngày, theo giờ VN) lúc hh:mm. */
    private Instant at(DayOfWeek day, int hour, int minute) {
        LocalDate date = LocalDate.now(clock.withZone(TimeZones.VIETNAM)).plusDays(2).with(TemporalAdjusters.nextOrSame(day));
        return date.atTime(LocalTime.of(hour, minute)).atZone(TimeZones.VIETNAM).toInstant();
    }
}
