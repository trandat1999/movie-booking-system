package com.moviebooking.modules.showtime;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.common.util.TimeZones;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** API tra cứu cho khách: không cần đăng nhập, chỉ thấy dữ liệu còn hiệu lực. */
class PublicCatalogIntegrationTest extends AbstractAdminIntegrationTest {

    @Test
    void moviesArePublicAndEndedMoviesAreHidden() throws Exception {
        long genre = createGenre();
        String showing = unique("Đang Chiếu");
        String ended = unique("Đã Ngừng");
        String slug = JsonPath.read(
                postJson("/api/v1/admin/movies", movieBody(showing, 100, "NOW_SHOWING", List.of(genre)))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.slug");
        String endedSlug = JsonPath.read(
                postJson("/api/v1/admin/movies", movieBody(ended, 100, "ENDED", List.of(genre)))
                        .andReturn().getResponse().getContentAsString(),
                "$.data.slug");

        mockMvc.perform(get("/api/v1/movies").param("genreId", String.valueOf(genre)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].title", hasItem(showing)))
                .andExpect(jsonPath("$.data.items[*].title", not(hasItem(ended))));
        mockMvc.perform(get("/api/v1/movies").param("status", "ENDED"))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        mockMvc.perform(get("/api/v1/movies/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value(showing));
        mockMvc.perform(get("/api/v1/movies/{slug}", endedSlug))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("MOVIE_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/movie-languages")).andExpect(jsonPath("$.data", hasItem("Tiếng Việt")));
        mockMvc.perform(get("/api/v1/genres")).andExpect(status().isOk());
    }

    @Test
    void inactiveCinemasAreHidden() throws Exception {
        long active = createCinema();
        long inactive = createCinema();
        putJson("/api/v1/admin/cinemas/{id}",
                        Map.of("name", unique("Rạp đóng"), "address", "x", "city", "Hà Nội", "status", "INACTIVE"),
                        inactive)
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cinemas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", hasItem((int) active)))
                .andExpect(jsonPath("$.data[*].id", not(hasItem((int) inactive))));
        mockMvc.perform(get("/api/v1/cinemas/{id}", inactive)).andExpect(status().isNotFound());
    }

    @Test
    void showtimesOfADayExcludeCancelledAndShowSeatAvailability() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        LocalDate day = LocalDate.now(clock.withZone(TimeZones.VIETNAM)).plusDays(3).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        Instant morning = day.atTime(9, 0).atZone(TimeZones.VIETNAM).toInstant();
        Instant evening = day.atTime(20, 0).atZone(TimeZones.VIETNAM).toInstant();
        long kept = idOf(createShowtime(movieId, roomId, morning).andReturn());
        long cancelled = idOf(createShowtime(movieId, roomId, evening).andReturn());
        mockMvc.perform(post("/api/v1/admin/showtimes/{id}/cancel", cancelled).header("Authorization", admin.bearer()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/showtimes")
                        .param("movieId", String.valueOf(movieId))
                        .param("date", day.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(kept))
                .andExpect(jsonPath("$.data[0].availableSeats").value(6))
                .andExpect(jsonPath("$.data[0].minPrice").value(75_000));
        mockMvc.perform(get("/api/v1/showtimes")
                        .param("movieId", String.valueOf(movieId))
                        .param("date", day.plusDays(1).toString()))
                .andExpect(jsonPath("$.data", hasSize(0)));
        mockMvc.perform(get("/api/v1/showtimes/{id}", cancelled)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/showtimes/{id}", kept))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.prices", hasSize(3)))
                .andExpect(jsonPath("$.data.cinemaAddress").isNotEmpty());
    }

    @Test
    void seatMapShowsLayoutPricesAndBlockedSeats() throws Exception {
        long movieId = createMovie(90);
        long roomId = createRoomWithLayout();
        Instant start = LocalDate.now(clock.withZone(TimeZones.VIETNAM)).plusDays(4).atTime(15, 0)
                .atZone(TimeZones.VIETNAM).toInstant();
        long showtimeId = idOf(createShowtime(movieId, roomId, start).andReturn());

        String layout = mockMvc.perform(get("/api/v1/admin/rooms/{id}", roomId).header("Authorization", admin.bearer()))
                .andReturn().getResponse().getContentAsString();
        long seatId = ((Number) JsonPath.read(layout, "$.data.seats[0].id")).longValue();
        mockMvc.perform(patch("/api/v1/admin/rooms/{r}/seats/{s}", roomId, seatId)
                        .header("Authorization", admin.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("active", false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.gridRows").value(2))
                .andExpect(jsonPath("$.data.gridCols").value(5))
                .andExpect(jsonPath("$.data.seats", hasSize(6)))
                .andExpect(jsonPath("$.data.seats[?(@.id == " + seatId + ")].status").value(hasItem("BLOCKED")))
                .andExpect(jsonPath("$.data.seatTypes[*].code", org.hamcrest.Matchers.contains("STANDARD", "VIP", "COUPLE")))
                .andExpect(jsonPath("$.data.seatTypes[2].price").value(180_000));
    }

    @Test
    void publicAccessDoesNotOpenAdminOrWriteEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/movies")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/showtimes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/movies").param("status", "NOW_SHOWING"))
                .andExpect(jsonPath("$.data.items[*].status", everyItem(org.hamcrest.Matchers.is("NOW_SHOWING"))));
    }
}
