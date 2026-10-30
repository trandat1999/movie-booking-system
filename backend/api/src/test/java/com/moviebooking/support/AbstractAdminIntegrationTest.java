package com.moviebooking.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.modules.user.entity.Role;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Test tích hợp cho API quản trị: có sẵn phiên ADMIN và các hàm tạo dữ liệu danh mục qua API. */
public abstract class AbstractAdminIntegrationTest extends AbstractIntegrationTest {

    protected Session admin;

    @BeforeEach
    void loginAdmin() throws Exception {
        admin = login(createUser(uniqueEmail("admin"), Role.ADMIN).getEmail(), PASSWORD);
    }

    protected ResultActions postJson(String url, Object body, Object... vars) throws Exception {
        return mockMvc.perform(post(url, vars)
                .header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    protected ResultActions putJson(String url, Object body, Object... vars) throws Exception {
        return mockMvc.perform(put(url, vars)
                .header("Authorization", admin.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    protected static long idOf(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    protected static String unique(String prefix) {
        return prefix + " " + UUID.randomUUID().toString().substring(0, 8);
    }

    protected long createGenre() throws Exception {
        return idOf(postJson("/api/v1/admin/genres", Map.of("name", unique("Thể loại")))
                .andExpect(status().isCreated())
                .andReturn());
    }

    protected long createPerson(String name) throws Exception {
        return idOf(postJson("/api/v1/admin/people", Map.of("fullName", name))
                .andExpect(status().isCreated())
                .andReturn());
    }

    protected Map<String, Object> movieBody(String title, int durationMin, String status, List<Long> genreIds) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("durationMin", durationMin);
        body.put("ageRating", "T13");
        body.put("language", "Tiếng Việt");
        body.put("releaseDate", LocalDate.of(2026, 10, 1).toString());
        body.put("status", status);
        body.put("genreIds", genreIds);
        return body;
    }

    protected long createMovie(int durationMin) throws Exception {
        return idOf(postJson("/api/v1/admin/movies", movieBody(unique("Phim"), durationMin, "NOW_SHOWING", List.of(createGenre())))
                .andExpect(status().isCreated())
                .andReturn());
    }

    protected long createCinema() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", unique("Rạp"));
        body.put("address", "1 Lê Lợi");
        body.put("city", "TP. Hồ Chí Minh");
        body.put("amenities", List.of("Bãi đỗ xe"));
        body.put("status", "ACTIVE");
        return idOf(postJson("/api/v1/admin/cinemas", body).andExpect(status().isCreated()).andReturn());
    }

    protected long createRoom(long cinemaId) throws Exception {
        return idOf(postJson(
                        "/api/v1/admin/cinemas/{id}/rooms",
                        Map.of("name", unique("Phòng"), "roomType", "STANDARD", "status", "ACTIVE"),
                        cinemaId)
                .andExpect(status().isCreated())
                .andReturn());
    }

    /**
     * Phòng 2 hàng × 5 cột: hàng A = 2 ghế thường | lối đi | 2 ghế VIP; hàng B = 2 ghế đôi (4 ô) + 1 ô trống.
     * Tổng 6 ghế: STANDARD ×2, VIP ×2, COUPLE ×2.
     */
    protected long createRoomWithLayout() throws Exception {
        long roomId = createRoom(createCinema());
        putJson("/api/v1/admin/rooms/{id}/layout", smallLayout(), roomId).andExpect(status().isOk());
        return roomId;
    }

    protected static Map<String, Object> smallLayout() {
        List<Map<String, Object>> seats = new ArrayList<>();
        seats.add(Map.of("gridRow", 0, "gridCol", 0, "seatTypeCode", "STANDARD"));
        seats.add(Map.of("gridRow", 0, "gridCol", 1, "seatTypeCode", "STANDARD"));
        seats.add(Map.of("gridRow", 0, "gridCol", 3, "seatTypeCode", "VIP"));
        seats.add(Map.of("gridRow", 0, "gridCol", 4, "seatTypeCode", "VIP"));
        seats.add(Map.of("gridRow", 1, "gridCol", 0, "seatTypeCode", "COUPLE"));
        seats.add(Map.of("gridRow", 1, "gridCol", 2, "seatTypeCode", "COUPLE"));
        return Map.of("gridRows", 2, "gridCols", 5, "seats", seats);
    }

    protected ResultActions createShowtime(long movieId, long roomId, Instant startAt) throws Exception {
        return postJson(
                "/api/v1/admin/showtimes",
                Map.of("movieId", movieId, "roomId", roomId, "startAt", startAt.toString(), "format", "TWO_D",
                        "audioType", "SUBTITLED"));
    }
}
