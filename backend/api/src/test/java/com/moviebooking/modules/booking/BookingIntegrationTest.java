package com.moviebooking.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.modules.booking.service.HoldService;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** Đặt vé cá nhân end-to-end: giữ ghế → tạo đơn → thanh toán giả lập → vé (M4). */
class BookingIntegrationTest extends AbstractAdminIntegrationTest {

    @Autowired
    private HoldService holdService;

    private Session alice;
    private Session bob;

    @BeforeEach
    void loginCustomers() throws Exception {
        alice = customer();
        bob = customer();
    }

    @Test
    void holdOrderAndPayIssuesTicketsAndSellsSeats() throws Exception {
        Fixture f = bookableShowtime();
        MvcResult holdResult = hold(alice, f, "A1", "A2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats.length()").value(2))
                .andExpect(jsonPath("$.data.total").value(2 * f.standardPrice()))
                .andReturn();
        long holdId = longAt(holdResult, "$.data.id");
        assertThat(seatStatus(f, "A1")).isEqualTo("HELD");

        String code = createOrder(alice, holdId);
        // Gọi lại với cùng lượt giữ → cùng đơn (idempotent)
        assertThat(createOrder(alice, holdId)).isEqualTo(code);

        Map<String, String> gateway = startPayment(alice, code);
        ipn(gateway, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));
        // IPN lặp lại không xử lý lần hai (BR05)
        ipn(gateway, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        perform(alice, get("/api/v1/orders/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.total").value(2 * f.standardPrice()))
                .andExpect(jsonPath("$.data.tickets.length()").value(2))
                .andExpect(jsonPath("$.data.tickets[0].seatLabel").value("A1"))
                .andExpect(jsonPath("$.data.tickets[0].qrImage").value(org.hamcrest.Matchers.startsWith(
                        "data:image/png;base64,")));
        assertThat(seatStatus(f, "A1")).isEqualTo("SOLD");
        assertThat(seatStatus(f, "A2")).isEqualTo("SOLD");

        perform(alice, get("/api/v1/me/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].orderCode").value(code));
        perform(alice, get("/api/v1/me/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value(code))
                .andExpect(jsonPath("$.data.items[0].seatCount").value(2));
        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].type").value("ORDER_PAID"))
                .andExpect(jsonPath("$.data.items[0].link").value("/orders/" + code));
        perform(alice, get("/api/v1/me/notifications/unread-count"))
                .andExpect(jsonPath("$.data.count").value(1));
        perform(alice, post("/api/v1/me/notifications/read-all")).andExpect(status().isOk());
        perform(alice, get("/api/v1/me/notifications/unread-count"))
                .andExpect(jsonPath("$.data.count").value(0));
    }

    @Test
    void seatsHeldByOthersCannotBeHeldAndNothingIsPartiallyHeld() throws Exception {
        Fixture f = bookableShowtime();
        hold(alice, f, "A1", "A2").andExpect(status().isOk());

        hold(bob, f, "A2", "A3")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SEAT_UNAVAILABLE"));
        assertThat(seatStatus(f, "A3")).isEqualTo("AVAILABLE");
    }

    @Test
    void concurrentHoldsOnSameSeatsOnlyOneSucceeds() throws Exception {
        Fixture f = bookableShowtime();
        int threads = 8;
        List<Session> users = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            users.add(customer());
        }
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                Session user = users.get(i);
                // Mỗi người chọn 2 ghế, chồng nhau từng cặp để thử cả trường hợp khóa chéo
                String[] seats = i % 2 == 0 ? new String[] {"B3", "B4"} : new String[] {"B4", "B3"};
                results.add(pool.submit(() -> {
                    start.await();
                    return hold(user, f, seats).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            int ok = 0;
            int conflict = 0;
            for (Future<Integer> result : results) {
                int code = result.get();
                if (code == 200) {
                    ok++;
                } else if (code == 409) {
                    conflict++;
                }
            }
            assertThat(ok).isEqualTo(1);
            assertThat(conflict).isEqualTo(threads - 1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(seatStatus(f, "B3")).isEqualTo("HELD");
        assertThat(seatStatus(f, "B4")).isEqualTo("HELD");
    }

    @Test
    void holdRulesAreEnforced() throws Exception {
        Fixture f = bookableShowtime();
        // A2 một mình để trống A1 ở mép dãy (quy tắc ghế lẻ = block)
        hold(alice, f, "A2")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ORPHAN_SEAT"))
                .andExpect(jsonPath("$.error.details.seats[0]").value("A1"));
        hold(alice, f, "A1", "A2", "A3", "A4", "A5", "A6", "B1", "B2", "B3")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_SEATS"));
        mockMvc.perform(post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", f.showtimeId(), "seatIds", List.of(f.seat("A1"))))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void newHoldReplacesPreviousHoldAndActiveHoldsAreLimited() throws Exception {
        Fixture f = bookableShowtime();
        long first = longAt(hold(alice, f, "A1", "A2").andExpect(status().isOk()).andReturn(), "$.data.id");
        String firstOrder = createOrder(alice, first);

        hold(alice, f, "B1", "B2").andExpect(status().isOk());
        assertThat(seatStatus(f, "A1")).isEqualTo("AVAILABLE");
        assertThat(seatStatus(f, "B1")).isEqualTo("HELD");
        perform(alice, get("/api/v1/orders/{code}", firstOrder)).andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // max_active_holds_per_user = 2
        hold(alice, bookableShowtime(), "A1", "A2").andExpect(status().isOk());
        hold(alice, bookableShowtime(), "A1", "A2")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TOO_MANY_ACTIVE_HOLDS"));
    }

    @Test
    void releaseAndCancelReturnSeats() throws Exception {
        Fixture f = bookableShowtime();
        long holdId = longAt(hold(alice, f, "A1", "A2").andExpect(status().isOk()).andReturn(), "$.data.id");
        perform(bob, delete("/api/v1/holds/{id}", holdId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HOLD_NOT_FOUND"));
        perform(alice, delete("/api/v1/holds/{id}", holdId)).andExpect(status().isOk());
        assertThat(seatStatus(f, "A1")).isEqualTo("AVAILABLE");

        long second = longAt(hold(alice, f, "A3", "A4").andExpect(status().isOk()).andReturn(), "$.data.id");
        String code = createOrder(alice, second);
        perform(alice, post("/api/v1/orders/{code}/cancel", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
        assertThat(seatStatus(f, "A3")).isEqualTo("AVAILABLE");
        perform(alice, post("/api/v1/orders/{code}/cancel", code))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_CANCELLABLE"));
    }

    @Test
    void ordersOfOtherUsersAreInvisible() throws Exception {
        Fixture f = bookableShowtime();
        long holdId = longAt(hold(alice, f, "A1", "A2").andExpect(status().isOk()).andReturn(), "$.data.id");
        perform(bob, post("/api/v1/orders").contentType(MediaType.APPLICATION_JSON).content(json(Map.of("holdId", holdId))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("HOLD_NOT_FOUND"));
        String code = createOrder(alice, holdId);
        perform(bob, get("/api/v1/orders/{code}", code))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_FOUND"));
        perform(bob, post("/api/v1/orders/{code}/pay", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("provider", "MOCK"))))
                .andExpect(status().isNotFound());
        perform(bob, post("/api/v1/orders/{code}/cancel", code)).andExpect(status().isNotFound());
    }

    @Test
    void invalidSignatureIsRejectedAndFailedPaymentCanBeRetried() throws Exception {
        Fixture f = bookableShowtime();
        String code = createOrder(alice, longAt(hold(alice, f, "A1", "A2").andReturn(), "$.data.id"));
        Map<String, String> gateway = startPayment(alice, code);

        Map<String, String> forged = new HashMap<>(gateway);
        forged.put("sigSuccess", gateway.get("sigFailed"));
        ipn(forged, true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_PAYMENT_SIGNATURE"));

        ipn(gateway, false).andExpect(status().isOk()).andExpect(jsonPath("$.data.orderStatus").value("PENDING_PAYMENT"));
        perform(alice, get("/api/v1/orders/{code}", code)).andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        ipn(startPayment(alice, code), true).andExpect(jsonPath("$.data.orderStatus").value("PAID"));
    }

    @Test
    void expiredHoldReleasesSeatsAndLatePaymentReacquiresThem() throws Exception {
        Fixture f = bookableShowtime();
        long holdId = longAt(hold(alice, f, "A1", "A2").andReturn(), "$.data.id");
        String code = createOrder(alice, holdId);
        Map<String, String> gateway = startPayment(alice, code);

        clock.advance(Duration.ofMinutes(8));
        assertThat(holdService.expire(holdId)).isTrue();
        assertThat(holdService.expire(holdId)).isFalse();
        perform(alice, get("/api/v1/orders/{code}", code)).andExpect(jsonPath("$.data.status").value("EXPIRED"));
        assertThat(seatStatus(f, "A1")).isEqualTo("AVAILABLE");
        perform(alice, post("/api/v1/orders/{code}/pay", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("provider", "MOCK"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ORDER_NOT_PAYABLE"));
        perform(alice, get("/api/v1/me/notifications")).andExpect(jsonPath("$.data.items[0].type").value("ORDER_EXPIRED"));

        // Tiền về sau khi hết hạn, ghế vẫn trống → nhận lại ghế (BR17)
        ipn(gateway, true).andExpect(jsonPath("$.data.orderStatus").value("PAID"));
        assertThat(seatStatus(f, "A1")).isEqualTo("SOLD");
    }

    @Test
    void latePaymentForSeatsTakenByOthersIsRefunded() throws Exception {
        Fixture f = bookableShowtime();
        long holdId = longAt(hold(alice, f, "A1", "A2").andReturn(), "$.data.id");
        String code = createOrder(alice, holdId);
        Map<String, String> gateway = startPayment(alice, code);

        clock.advance(Duration.ofMinutes(8));
        // Chưa có job dọn: ghế giữ quá hạn vẫn được coi là trống với người khác
        hold(bob, f, "A1", "A2").andExpect(status().isOk());

        ipn(gateway, true).andExpect(jsonPath("$.data.orderStatus").value("REFUNDED"));
        assertThat(seatStatus(f, "A1")).isEqualTo("HELD");
        perform(alice, get("/api/v1/orders/{code}", code))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.refund.amount").value(2 * f.standardPrice()))
                .andExpect(jsonPath("$.data.tickets.length()").value(0));
    }

    // --- helpers ---

    private record Fixture(long showtimeId, Map<String, Long> seats, long standardPrice) {

        long seat(String label) {
            return seats.get(label);
        }
    }

    /** Suất chiếu 3 ngày sau ở phòng mới: 2 hàng × 6 ghế thường liền nhau (A1–A6, B1–B6). */
    private Fixture bookableShowtime() throws Exception {
        long roomId = createRoom(createCinema());
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                layout.add(Map.of("gridRow", row, "gridCol", col, "seatTypeCode", "STANDARD"));
            }
        }
        putJson("/api/v1/admin/rooms/{id}/layout", Map.of("gridRows", 2, "gridCols", 6, "seats", layout), roomId)
                .andExpect(status().isOk());
        Instant startAt = clock.instant().plus(Duration.ofDays(3)).truncatedTo(ChronoUnit.HOURS);
        long showtimeId = idOf(createShowtime(createMovie(100), roomId, startAt)
                .andExpect(status().isCreated())
                .andReturn());

        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Long> seats = new HashMap<>();
        List<Map<String, Object>> seatList = JsonPath.read(body, "$.data.seats");
        for (Map<String, Object> seat : seatList) {
            seats.put(seat.get("rowLabel") + String.valueOf(seat.get("number")), ((Number) seat.get("id")).longValue());
        }
        long price = ((Number) JsonPath.read(body, "$.data.seatTypes[0].price")).longValue();
        return new Fixture(showtimeId, seats, price);
    }

    private String seatStatus(Fixture f, String label) throws Exception {
        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", f.showtimeId()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> statuses = JsonPath.read(body, "$.data.seats[?(@.id == " + f.seat(label) + ")].status");
        return statuses.get(0);
    }

    private Session customer() throws Exception {
        return login(createUser(uniqueEmail("customer"), Role.CUSTOMER).getEmail(), PASSWORD);
    }

    private ResultActions perform(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", session.bearer()));
    }

    private ResultActions hold(Session session, Fixture f, String... labels) throws Exception {
        List<Long> ids = java.util.Arrays.stream(labels).map(f::seat).toList();
        return perform(session, post("/api/v1/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("showtimeId", f.showtimeId(), "seatIds", ids))));
    }

    private String createOrder(Session session, long holdId) throws Exception {
        MvcResult result = perform(session, post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("holdId", holdId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.code");
    }

    /** Bắt đầu thanh toán MOCK, trả về tham số trang cổng giả lập (orderCode, transactionId, amount, sig...). */
    private Map<String, String> startPayment(Session session, String code) throws Exception {
        MvcResult result = perform(session, post("/api/v1/orders/{code}/pay", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("provider", "MOCK"))))
                .andExpect(status().isOk())
                .andReturn();
        String url = JsonPath.read(result.getResponse().getContentAsString(), "$.data.redirectUrl");
        Map<String, String> params = new HashMap<>();
        UriComponentsBuilder.fromUriString("http://gateway" + url)
                .build()
                .getQueryParams()
                .forEach((key, values) -> params.put(key, URLDecoder.decode(values.get(0), StandardCharsets.UTF_8)));
        return params;
    }

    /** Trang cổng giả lập gửi IPN (không cần đăng nhập). */
    private ResultActions ipn(Map<String, String> gateway, boolean success) throws Exception {
        Map<String, String> body = Map.of(
                "orderCode", gateway.get("orderCode"),
                "transactionId", gateway.get("transactionId"),
                "amount", gateway.get("amount"),
                "result", success ? "SUCCESS" : "FAILED",
                "signature", gateway.get(success ? "sigSuccess" : "sigFailed"));
        return mockMvc.perform(post("/api/v1/payments/MOCK/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private static long longAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }
}
