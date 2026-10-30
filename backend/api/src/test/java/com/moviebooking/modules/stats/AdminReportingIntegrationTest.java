package com.moviebooking.modules.stats;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.common.util.TimeZones;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** M8: mã giảm giá, dashboard khớp dữ liệu, thống kê nhóm, xuất CSV, quản trị đơn và nhóm. */
class AdminReportingIntegrationTest extends AbstractAdminIntegrationTest {

    private Session alice;
    private Session bob;
    private String aliceEmail;

    @BeforeEach
    void loginCustomers() throws Exception {
        aliceEmail = uniqueEmail("alice");
        alice = login(createUser(aliceEmail, Role.CUSTOMER).getEmail(), PASSWORD);
        bob = login(createUser(uniqueEmail("bob"), Role.CUSTOMER).getEmail(), PASSWORD);
    }

    @Test
    void groupPromotionNeedsEnoughTicketsAndHasLimitedUses() throws Exception {
        Fixture f = showtime();
        String code = "NHOM" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        postJson("/api/v1/admin/promotions", promotion(code, "PERCENT", 10, 50_000L, 3, 1)).andExpect(status().isCreated());
        postJson("/api/v1/admin/promotions", promotion(code, "AMOUNT", 10, null, 1, null))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_CODE_EXISTS"));

        String small = order(alice, f, "A1", "A2");
        applyPromotion(alice, small, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_APPLICABLE"));

        String big = order(bob, f, "B1", "B2", "B3");
        long subtotal = 3 * f.price();
        long discount = Math.min(subtotal / 10, 50_000);
        applyPromotion(bob, big, code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.promotionCode").value(code))
                .andExpect(jsonPath("$.data.discount").value(discount))
                .andExpect(jsonPath("$.data.total").value(subtotal - discount));
        // usage_limit = 1: lượt duy nhất đang được đơn của Bob giữ
        Session carol = login(createUser(uniqueEmail("carol"), Role.CUSTOMER).getEmail(), PASSWORD);
        String other = order(carol, f, "B4", "B5", "B6");
        applyPromotion(carol, other, code).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PROMOTION_USED_UP"));

        // Bỏ mã → trả lượt, người khác dùng được
        perform(bob, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                        "/api/v1/orders/{code}/promotion", big))
                .andExpect(jsonPath("$.data.total").value(subtotal));
        applyPromotion(carol, other, code).andExpect(status().isOk());
        // Thanh toán đúng số tiền sau giảm
        pay(carol, other).andExpect(jsonPath("$.data.orderStatus").value("PAID"));
        mockMvc.perform(get("/api/v1/admin/promotions").param("q", code).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.items[0].usedCount").value(1));
        mockMvc.perform(get("/api/v1/admin/promotions").header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void dashboardNumbersMatchTheData() throws Exception {
        Fixture f = showtime();
        String paid = order(alice, f, "A1", "A2");
        Map<String, String> first = gateway(alice, paid);
        ipn(first, false).andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));
        pay(alice, paid).andExpect(jsonPath("$.data.orderStatus").value("PAID"));
        String refunded = order(bob, f, "B1", "B2", "B3");
        pay(bob, refunded);
        perform(bob, post("/api/v1/orders/{code}/cancel", refunded)).andExpect(jsonPath("$.data.status").value("REFUNDED"));

        LocalDate today = LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        String range = "from=" + today + "&to=" + today.plusDays(5) + "&movieId=" + f.movieId();
        mockMvc.perform(get("/api/v1/admin/stats/overview?" + range).header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orders").value(1))
                .andExpect(jsonPath("$.data.revenue").value(2 * f.price()))
                .andExpect(jsonPath("$.data.ticketsSold").value(2))
                .andExpect(jsonPath("$.data.avgOrderValue").value(2 * f.price()))
                .andExpect(jsonPath("$.data.refundCount").value(1))
                .andExpect(jsonPath("$.data.refundTotal").value(3 * f.price()))
                .andExpect(jsonPath("$.data.paymentAttempts").value(3))
                .andExpect(jsonPath("$.data.paymentFailed").value(1))
                .andExpect(jsonPath("$.data.seatsSold").value(2))
                .andExpect(jsonPath("$.data.seatsTotal").value(12))
                .andExpect(jsonPath("$.data.daily[0].revenue").value(2 * f.price()))
                .andExpect(jsonPath("$.data.topMovies[0].id").value(f.movieId()));

        MvcResult csv = mockMvc.perform(get("/api/v1/admin/reports/orders.csv?" + range).header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andReturn();
        byte[] bytes = csv.getResponse().getContentAsByteArray();
        assertThat(bytes[0]).isEqualTo((byte) 0xEF);
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertThat(text).contains("Mã đơn,").contains(paid).contains(refunded).contains(aliceEmail);

        mockMvc.perform(get("/api/v1/admin/stats/overview").header("Authorization", alice.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminSearchesOrdersAndRefundsOne() throws Exception {
        Fixture f = showtime();
        String code = order(alice, f, "A1", "A2");
        pay(alice, code);

        mockMvc.perform(get("/api/v1/admin/orders").param("q", code.substring(2, 8)).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.items[?(@.code == '" + code + "')].customer.email").value(aliceEmail));
        mockMvc.perform(get("/api/v1/admin/orders").param("email", aliceEmail).param("status", "PAID")
                        .header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        mockMvc.perform(get("/api/v1/admin/orders/{code}", code).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.order.status").value("PAID"))
                .andExpect(jsonPath("$.data.events[0].type").value("CREATED"))
                .andExpect(jsonPath("$.data.events[?(@.type == 'PAID')]").exists())
                .andExpect(jsonPath("$.data.payments[0].status").value("SUCCEEDED"));

        // STAFF xem được đơn nhưng không có quyền hoàn tiền
        Session staff = login(createUser(uniqueEmail("staff"), Role.STAFF).getEmail(), PASSWORD);
        mockMvc.perform(get("/api/v1/admin/orders/{code}", code).header("Authorization", staff.bearer()))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/{code}/cancel", code).header("Authorization", staff.bearer()))
                .andExpect(status().isForbidden());
        postJson("/api/v1/admin/orders/{code}/cancel", Map.of(), code)
                .andExpect(jsonPath("$.data.order.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.order.refund.amount").value(2 * f.price()));
        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(jsonPath("$.data.items[?(@.type == 'ORDER_REFUNDED')]").exists());
    }

    @Test
    void groupStatsAndAdminGroupDetail() throws Exception {
        Fixture f = showtime();
        long groupId = idOf(perform(alice, post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nhóm thống kê", "movieId", f.movieId(),
                                "rsvpDeadline", clock.instant().plus(Duration.ofDays(1)).toString()))))
                .andExpect(status().isCreated())
                .andReturn());
        String invite = JsonPath.read(perform(alice, get("/api/v1/groups/{id}", groupId)).andReturn().getResponse().getContentAsString(),
                "$.data.code");
        perform(bob, post("/api/v1/groups/join").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("code", invite))));
        perform(alice, post("/api/v1/groups/{id}/lock", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("showtimeId", f.showtimeId(), "ticketCount", 2))));
        MvcResult suggestions = perform(alice, post("/api/v1/groups/{id}/seat-suggestions", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("allowSplit", true))))
                .andReturn();
        String body = suggestions.getResponse().getContentAsString();
        List<Integer> seats = JsonPath.read(body, "$.data.options[0].seatIds");
        Map<String, Object> hold = new HashMap<>();
        hold.put("seatIds", seats);
        hold.put("suggestionLogId", ((Number) JsonPath.read(body, "$.data.logId")).longValue());
        hold.put("chosenRank", 1);
        perform(alice, post("/api/v1/groups/{id}/hold", groupId).contentType(MediaType.APPLICATION_JSON).content(json(hold)))
                .andExpect(status().isOk());
        String code = JsonPath.read(perform(alice, post("/api/v1/groups/{id}/order", groupId)).andReturn().getResponse()
                .getContentAsString(), "$.data.code");
        pay(alice, code).andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        LocalDate today = LocalDate.now(clock.withZone(TimeZones.VIETNAM));
        mockMvc.perform(get("/api/v1/admin/stats/groups?from=" + today + "&to=" + today + "&movieId=" + f.movieId())
                        .header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.totalGroups").value(1))
                .andExpect(jsonPath("$.data.booked").value(1))
                .andExpect(jsonPath("$.data.conversionRate").value(1.0))
                .andExpect(jsonPath("$.data.avgTickets").value(2.0))
                .andExpect(jsonPath("$.data.avgMembers").value(2.0))
                .andExpect(jsonPath("$.data.suggestions.count").value(1))
                .andExpect(jsonPath("$.data.suggestions.contiguousRate").value(1.0))
                .andExpect(jsonPath("$.data.suggestions.topChoiceRate").value(1.0))
                .andExpect(jsonPath("$.data.avgHoursToBook").isNumber());

        mockMvc.perform(get("/api/v1/admin/groups").param("q", invite).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.items[0].id").value(groupId))
                .andExpect(jsonPath("$.data.items[0].memberCount").value(2));
        mockMvc.perform(get("/api/v1/admin/groups/{id}", groupId).header("Authorization", admin.bearer()))
                .andExpect(jsonPath("$.data.group.status").value("BOOKED"))
                .andExpect(jsonPath("$.data.events[0].type").value("CREATED"))
                .andExpect(jsonPath("$.data.suggestions[0].chosenRank").value(1));

        // Admin hủy đơn nhóm → nhóm cũng kết thúc
        postJson("/api/v1/admin/orders/{code}/cancel", Map.of(), code).andExpect(status().isOk());
        perform(alice, get("/api/v1/groups/{id}", groupId)).andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    // --- helpers ---

    private record Fixture(long showtimeId, long movieId, Map<String, Long> seats, long price) {}

    private Fixture showtime() throws Exception {
        long roomId = createRoom(createCinema());
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                layout.add(Map.of("gridRow", row, "gridCol", col, "seatTypeCode", "STANDARD"));
            }
        }
        putJson("/api/v1/admin/rooms/{id}/layout", Map.of("gridRows", 2, "gridCols", 6, "seats", layout), roomId)
                .andExpect(status().isOk());
        long movieId = createMovie(100);
        Instant startAt = clock.instant().plus(Duration.ofDays(3)).truncatedTo(ChronoUnit.HOURS);
        long showtimeId = idOf(createShowtime(movieId, roomId, startAt).andExpect(status().isCreated()).andReturn());
        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId)).andReturn().getResponse().getContentAsString();
        Map<String, Long> seats = new HashMap<>();
        List<Map<String, Object>> list = JsonPath.read(body, "$.data.seats");
        list.forEach(s -> seats.put(s.get("rowLabel") + String.valueOf(s.get("number")), ((Number) s.get("id")).longValue()));
        return new Fixture(showtimeId, movieId, seats, ((Number) JsonPath.read(body, "$.data.seatTypes[0].price")).longValue());
    }

    private Map<String, Object> promotion(String code, String type, long value, Long maxDiscount, int minTickets, Integer limit) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", code);
        body.put("name", "Ưu đãi nhóm");
        body.put("type", type);
        body.put("value", value);
        body.put("maxDiscount", maxDiscount);
        body.put("minOrder", 0);
        body.put("minTickets", minTickets);
        body.put("startsAt", clock.instant().minus(Duration.ofDays(1)).toString());
        body.put("endsAt", clock.instant().plus(Duration.ofDays(30)).toString());
        body.put("usageLimit", limit);
        body.put("status", "ACTIVE");
        return body;
    }

    private String order(Session session, Fixture f, String... labels) throws Exception {
        List<Long> ids = java.util.Arrays.stream(labels).map(f.seats()::get).toList();
        MvcResult hold = perform(session, post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", f.showtimeId(), "seatIds", ids))))
                .andExpect(status().isOk())
                .andReturn();
        long holdId = ((Number) JsonPath.read(hold.getResponse().getContentAsString(), "$.data.id")).longValue();
        return JsonPath.read(perform(session, post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("holdId", holdId))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.code");
    }

    private ResultActions applyPromotion(Session session, String order, String code) throws Exception {
        return perform(session, post("/api/v1/orders/{code}/promotion", order)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("code", code))));
    }

    private Map<String, String> gateway(Session session, String code) throws Exception {
        String url = JsonPath.read(perform(session, post("/api/v1/orders/{code}/pay", code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("provider", "MOCK"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.redirectUrl");
        Map<String, String> params = new HashMap<>();
        UriComponentsBuilder.fromUriString("http://gateway" + url).build().getQueryParams()
                .forEach((k, v) -> params.put(k, URLDecoder.decode(v.get(0), StandardCharsets.UTF_8)));
        return params;
    }

    private ResultActions pay(Session session, String code) throws Exception {
        return ipn(gateway(session, code), true);
    }

    private ResultActions ipn(Map<String, String> p, boolean success) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/MOCK/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "orderCode", p.get("orderCode"),
                        "transactionId", p.get("transactionId"),
                        "amount", p.get("amount"),
                        "result", success ? "SUCCESS" : "FAILED",
                        "signature", p.get(success ? "sigSuccess" : "sigFailed")))));
    }

    private ResultActions perform(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", session.bearer()));
    }
}
