package com.moviebooking.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Rà soát bảo mật M9: người dùng không truy cập được tài nguyên của người khác (IDOR) qua mã đơn / id lượt giữ /
 * id nhóm / id thông báo đoán được; khách hàng không gọi được API quản trị; API riêng tư yêu cầu đăng nhập.
 * Trả 404 (không phải 403) để không lộ sự tồn tại của tài nguyên.
 */
class AccessControlIntegrationTest extends AbstractAdminIntegrationTest {

    private Session alice;
    private Session mallory;
    private long showtimeId;
    private long movieId;
    private final Map<String, Long> seats = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        alice = login(createUser(uniqueEmail("alice"), Role.CUSTOMER).getEmail(), PASSWORD);
        mallory = login(createUser(uniqueEmail("mallory"), Role.CUSTOMER).getEmail(), PASSWORD);
        long roomId = createRoom(createCinema());
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int col = 0; col < 6; col++) {
            layout.add(Map.of("gridRow", 0, "gridCol", col, "seatTypeCode", "STANDARD"));
        }
        putJson("/api/v1/admin/rooms/{id}/layout", Map.of("gridRows", 1, "gridCols", 6, "seats", layout), roomId)
                .andExpect(status().isOk());
        movieId = createMovie(100);
        Instant startAt = clock.instant().plus(Duration.ofDays(2)).truncatedTo(ChronoUnit.MINUTES);
        showtimeId = idOf(createShowtime(movieId, roomId, startAt).andExpect(status().isCreated()).andReturn());
        List<Map<String, Object>> list = JsonPath.read(
                mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId)).andReturn().getResponse().getContentAsString(),
                "$.data.seats");
        list.forEach(s -> seats.put(s.get("rowLabel") + String.valueOf(s.get("number")), ((Number) s.get("id")).longValue()));
    }

    @Test
    void ordersAndHoldsOfAnotherUserAreInvisible() throws Exception {
        long holdId = holdId(alice, "A1", "A2");
        String code = JsonPath.read(
                perform(alice, post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("holdId", holdId))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.code");

        perform(mallory, get("/api/v1/orders/{code}", code)).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/orders/{code}/cancel", code)).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/orders/{code}/pay", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("provider", "MOCK"))))
                .andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/orders/{code}/promotion", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("code", "ANY"))))
                .andExpect(status().isNotFound());
        perform(mallory, delete("/api/v1/orders/{code}/promotion", code)).andExpect(status().isNotFound());
        // Lượt giữ của người khác: không trả được, không tạo đơn được
        perform(mallory, delete("/api/v1/holds/{id}", holdId)).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("holdId", holdId))))
                .andExpect(status().isNotFound());
        perform(mallory, get("/api/v1/me/orders")).andExpect(jsonPath("$.data.totalElements").value(0));

        // Chủ đơn vẫn dùng bình thường
        perform(alice, get("/api/v1/orders/{code}", code)).andExpect(status().isOk());
    }

    @Test
    void groupIsVisibleToMembersOnly() throws Exception {
        String deadline = clock.instant().plus(Duration.ofDays(1)).toString();
        long groupId = idOf(perform(alice, post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nhóm riêng", "movieId", movieId, "rsvpDeadline", deadline))))
                .andExpect(status().isCreated())
                .andReturn());

        perform(mallory, get("/api/v1/groups/{id}", groupId)).andExpect(status().isNotFound());
        perform(mallory, get("/api/v1/groups/{id}/summary", groupId)).andExpect(status().isNotFound());
        perform(mallory, patch("/api/v1/groups/{id}", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Bị sửa", "rsvpDeadline", deadline))))
                .andExpect(status().isNotFound());
        perform(mallory, put("/api/v1/groups/{id}/rsvp", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("rsvp", "YES", "guestCount", 0))))
                .andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/groups/{id}/options", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", showtimeId))))
                .andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/groups/{id}/cancel", groupId)).andExpect(status().isNotFound());
        perform(mallory, post("/api/v1/groups/{id}/hold", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("seatIds", List.of(seats.get("A1"))))))
                .andExpect(status().isNotFound());
        perform(mallory, get("/api/v1/me/groups")).andExpect(jsonPath("$.data.length()").value(0));
        perform(alice, get("/api/v1/groups/{id}", groupId)).andExpect(jsonPath("$.data.name").value("Nhóm riêng"));
    }

    @Test
    void notificationsOfAnotherUserCannotBeMarkedRead() throws Exception {
        long holdId = holdId(alice, "A1", "A2");
        perform(alice, delete("/api/v1/holds/{id}", holdId)).andExpect(status().isOk());
        // Dùng id lớn bất kỳ: không thuộc mallory → 404
        perform(mallory, post("/api/v1/me/notifications/{id}/read", Long.MAX_VALUE)).andExpect(status().isNotFound());
    }

    @Test
    void customerCannotCallAdminOrStaffApis() throws Exception {
        for (String url : List.of(
                "/api/v1/admin/orders",
                "/api/v1/admin/users",
                "/api/v1/admin/stats/overview",
                "/api/v1/admin/audit-logs",
                "/api/v1/admin/refunds",
                "/api/v1/admin/groups",
                "/api/v1/admin/promotions",
                "/api/v1/admin/reports/orders.csv")) {
            perform(alice, get(url)).andExpect(status().isForbidden());
        }
        perform(alice, post("/api/v1/admin/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("payload", "x.y"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void privateApisRequireAuthentication() throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(
                get("/api/v1/me/orders"),
                get("/api/v1/me/tickets"),
                get("/api/v1/me/groups"),
                get("/api/v1/me/notifications"),
                get("/api/v1/orders/MB00000000"),
                post("/api/v1/holds").contentType(MediaType.APPLICATION_JSON).content("{}"),
                get("/api/v1/admin/orders"))) {
            mockMvc.perform(request).andExpect(status().isUnauthorized());
        }
        // Token giả mạo
        mockMvc.perform(get("/api/v1/me/orders").header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.e30.x"))
                .andExpect(status().isUnauthorized());
    }

    private long holdId(Session session, String... labels) throws Exception {
        ResultActions result = perform(session, post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "showtimeId", showtimeId,
                                "seatIds", java.util.Arrays.stream(labels).map(seats::get).toList()))))
                .andExpect(status().isOk());
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.data.id")).longValue();
    }

    private ResultActions perform(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", session.bearer()));
    }
}
