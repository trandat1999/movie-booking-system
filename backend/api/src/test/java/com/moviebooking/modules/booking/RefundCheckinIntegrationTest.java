package com.moviebooking.modules.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.infrastructure.qr.TicketSigner;
import com.moviebooking.modules.booking.service.OrderService;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.support.AbstractAdminIntegrationTest;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** M7: hủy vé + hoàn tiền, rạp hủy suất, soát vé, VNPay, hoàn tất đơn. */
class RefundCheckinIntegrationTest extends AbstractAdminIntegrationTest {

    private static final String VNPAY_SECRET = "test-vnpay-secret";

    @Autowired
    private TicketSigner ticketSigner;

    @Autowired
    private OrderService orderService;

    private Session alice;
    private Session staff;

    @BeforeEach
    void loginUsers() throws Exception {
        alice = login(createUser(uniqueEmail("alice"), Role.CUSTOMER).getEmail(), PASSWORD);
        staff = login(createUser(uniqueEmail("staff"), Role.STAFF).getEmail(), PASSWORD);
    }

    @Test
    void customerCancelsPaidOrderAndIsRefunded() throws Exception {
        Fixture f = showtime(Duration.ofDays(3));
        String code = paidOrder(alice, f, "A1", "A2");

        perform(alice, get("/api/v1/orders/{code}", code))
                .andExpect(jsonPath("$.data.cancellableUntil").isNotEmpty())
                .andExpect(jsonPath("$.data.qrImage").isNotEmpty());
        perform(alice, post("/api/v1/orders/{code}/cancel", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.refund.amount").value(2 * f.price()))
                .andExpect(jsonPath("$.data.tickets[0].status").value("CANCELLED"));
        assertThat(seatStatus(f, "A1")).isEqualTo("AVAILABLE");
        perform(alice, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(0));
        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(jsonPath("$.data.items[?(@.type == 'ORDER_CANCELLED')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.type == 'ORDER_REFUNDED')]").exists());
        // Ghế đã hủy có thể bán lại
        paidOrder(login(createUser(uniqueEmail("bob"), Role.CUSTOMER).getEmail(), PASSWORD), f, "A1", "A2");
    }

    @Test
    void cancellationIsRefusedCloseToShowtime() throws Exception {
        // cancel_before_hours = 24: suất sau 10 giờ không còn được tự hủy
        Fixture f = showtime(Duration.ofHours(10));
        String code = paidOrder(alice, f, "A1", "A2");
        perform(alice, post("/api/v1/orders/{code}/cancel", code))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CANCEL_DEADLINE_PASSED"));
    }

    @Test
    void cinemaCancelsShowtimeAndEveryoneIsRefunded() throws Exception {
        Fixture f = showtime(Duration.ofHours(10));
        String paid = paidOrder(alice, f, "A1", "A2");
        Session bob = login(createUser(uniqueEmail("bob"), Role.CUSTOMER).getEmail(), PASSWORD);
        long bobHold = longAt(hold(bob, f, "B1", "B2").andReturn(), "$.data.id");
        String pending = createOrder(bob, bobHold);

        postJson("/api/v1/admin/showtimes/{id}/cancel", Map.of(), f.showtimeId()).andExpect(status().isOk());

        perform(alice, get("/api/v1/orders/{code}", paid))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refund.amount").value(2 * f.price()));
        perform(bob, get("/api/v1/orders/{code}", pending)).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(jsonPath("$.data.items[?(@.title == 'Suất chiếu đã bị hủy')]").exists());
        postJson("/api/v1/admin/showtimes/{id}/cancel", Map.of(), f.showtimeId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SHOWTIME_ALREADY_STARTED"));
    }

    @Test
    void ticketCanBeCheckedInOnlyOnce() throws Exception {
        Fixture f = showtime(Duration.ofHours(10));
        String code = paidOrder(alice, f, "A1", "A2");
        List<String> tickets = JsonPath.read(
                perform(alice, get("/api/v1/orders/{code}", code)).andReturn().getResponse().getContentAsString(),
                "$.data.tickets[*].code");
        String ticketQr = ticketSigner.sign(tickets.get(0));

        // Chưa tới giờ soát vé (mở trước 2 giờ)
        checkin(staff, ticketQr).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CHECKIN_NOT_OPEN"));
        clock.advance(Duration.ofHours(9));
        Session staffNow = login(email(staff), PASSWORD);
        checkin(staffNow, ticketQr)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedIn").value(1))
                .andExpect(jsonPath("$.data.orderCode").value(code));
        checkin(staffNow, ticketQr)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TICKET_ALREADY_USED"));
        // QR của đơn soát nốt vé còn lại; quét lại bị từ chối
        checkin(staffNow, ticketSigner.sign(code)).andExpect(jsonPath("$.data.checkedIn").value(1));
        checkin(staffNow, ticketSigner.sign(code)).andExpect(status().isConflict());
        checkin(staffNow, tickets.get(0) + ".forged").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TICKET_QR"));
        Session customer = login(email(alice), PASSWORD);
        checkin(customer, ticketQr).andExpect(status().isForbidden());

        // Đã soát thì không tự hủy được; suất chiếu xong → đơn hoàn tất
        clock.advance(Duration.ofHours(4));
        assertThat(orderService.completeFinishedOrders()).isGreaterThanOrEqualTo(1);
        perform(login(email(alice), PASSWORD), get("/api/v1/orders/{code}", code))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void vnpayPaymentAndManualRefund() throws Exception {
        Fixture f = showtime(Duration.ofDays(3));
        long holdId = longAt(hold(alice, f, "A1", "A2").andReturn(), "$.data.id");
        String code = createOrder(alice, holdId);

        perform(alice, get("/api/v1/payments/providers"))
                .andExpect(jsonPath("$.data[?(@.code == 'VNPAY')]").exists());
        String url = JsonPath.read(perform(alice, post("/api/v1/orders/{code}/pay", code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("provider", "VNPAY"))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.redirectUrl");
        assertThat(url).startsWith("https://sandbox.vnpayment.vn/").contains("vnp_SecureHash=");
        Map<String, String> request = query(url);
        assertThat(request.get("vnp_Amount")).isEqualTo(String.valueOf(2 * f.price() * 100));
        String expected = sign(withoutHash(request));
        assertThat(request.get("vnp_SecureHash")).isEqualTo(expected);

        // VNPay gọi IPN (GET); chữ ký sai → 97, đúng → 00, gọi lại → vẫn 00 (idempotent)
        Map<String, String> ipn = new TreeMap<>();
        ipn.put("vnp_Amount", request.get("vnp_Amount"));
        ipn.put("vnp_TmnCode", request.get("vnp_TmnCode"));
        ipn.put("vnp_TxnRef", request.get("vnp_TxnRef"));
        ipn.put("vnp_TransactionNo", "14123456");
        ipn.put("vnp_ResponseCode", "00");
        ipn.put("vnp_TransactionStatus", "00");
        ipn.put("vnp_BankCode", "NCB");
        ipn.put("vnp_OrderInfo", request.get("vnp_OrderInfo"));
        vnpayIpn(ipn, "deadbeef").andExpect(jsonPath("$.RspCode").value("97"));
        vnpayIpn(ipn, sign(ipn)).andExpect(jsonPath("$.RspCode").value("00"));
        vnpayIpn(ipn, sign(ipn)).andExpect(jsonPath("$.RspCode").value("00"));
        perform(alice, get("/api/v1/orders/{code}", code)).andExpect(jsonPath("$.data.status").value("PAID"));

        // VNPay không hoàn tự động → yêu cầu PENDING, admin hoàn thủ công rồi xác nhận
        perform(alice, post("/api/v1/orders/{code}/cancel", code))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.refund.status").value("PENDING"));
        MvcResult refunds = mockMvc.perform(get("/api/v1/admin/refunds")
                        .param("status", "PENDING")
                        .header("Authorization", admin.bearer()))
                .andExpect(status().isOk())
                .andReturn();
        List<Integer> ids = JsonPath.read(refunds.getResponse().getContentAsString(),
                "$.data.items[?(@.orderCode == '" + code + "')].id");
        assertThat(ids).hasSize(1);
        postJson("/api/v1/admin/refunds/{id}/complete", Map.of("note", "Hoàn qua merchant VNPay"), ids.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
        perform(alice, get("/api/v1/orders/{code}", code)).andExpect(jsonPath("$.data.status").value("REFUNDED"));
        postJson("/api/v1/admin/refunds/{id}/complete", Map.of(), ids.get(0))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REFUND_NOT_PENDING"));
        mockMvc.perform(get("/api/v1/admin/refunds").header("Authorization", staff.bearer()))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private record Fixture(long showtimeId, Map<String, Long> seats, long price) {
        long seat(String label) {
            return seats.get(label);
        }
    }

    private final Map<Session, String> emails = new HashMap<>();

    private String email(Session session) {
        return emails.get(session);
    }

    @Override
    protected Session login(String identifier, String password) throws Exception {
        Session session = super.login(identifier, password);
        emails.put(session, identifier);
        return session;
    }

    /** Suất ở phòng mới (2 × 6 ghế thường) bắt đầu sau {@code startIn}. */
    private Fixture showtime(Duration startIn) throws Exception {
        long roomId = createRoom(createCinema());
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 6; col++) {
                layout.add(Map.of("gridRow", row, "gridCol", col, "seatTypeCode", "STANDARD"));
            }
        }
        putJson("/api/v1/admin/rooms/{id}/layout", Map.of("gridRows", 2, "gridCols", 6, "seats", layout), roomId)
                .andExpect(status().isOk());
        Instant startAt = clock.instant().plus(startIn).truncatedTo(ChronoUnit.MINUTES);
        long showtimeId = idOf(createShowtime(createMovie(100), roomId, startAt).andExpect(status().isCreated()).andReturn());
        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Long> seats = new HashMap<>();
        List<Map<String, Object>> list = JsonPath.read(body, "$.data.seats");
        list.forEach(s -> seats.put(s.get("rowLabel") + String.valueOf(s.get("number")), ((Number) s.get("id")).longValue()));
        return new Fixture(showtimeId, seats, ((Number) JsonPath.read(body, "$.data.seatTypes[0].price")).longValue());
    }

    private String paidOrder(Session session, Fixture f, String... labels) throws Exception {
        String code = createOrder(session, longAt(hold(session, f, labels).andExpect(status().isOk()).andReturn(), "$.data.id"));
        String url = JsonPath.read(perform(session, post("/api/v1/orders/{code}/pay", code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("provider", "MOCK"))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.redirectUrl");
        Map<String, String> p = query("http://gateway" + url);
        mockMvc.perform(post("/api/v1/payments/MOCK/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "orderCode", p.get("orderCode"),
                                "transactionId", p.get("transactionId"),
                                "amount", p.get("amount"),
                                "result", "SUCCESS",
                                "signature", p.get("sigSuccess")))))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));
        return code;
    }

    private ResultActions hold(Session session, Fixture f, String... labels) throws Exception {
        List<Long> ids = java.util.Arrays.stream(labels).map(f::seat).toList();
        return perform(session, post("/api/v1/holds")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("showtimeId", f.showtimeId(), "seatIds", ids))));
    }

    private String createOrder(Session session, long holdId) throws Exception {
        return JsonPath.read(perform(session, post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("holdId", holdId))))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.code");
    }

    private ResultActions checkin(Session session, String payload) throws Exception {
        return perform(session, post("/api/v1/admin/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("payload", payload))));
    }

    private ResultActions vnpayIpn(Map<String, String> params, String signature) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/v1/payments/VNPAY/ipn");
        params.forEach(request::param);
        return mockMvc.perform(request.param("vnp_SecureHash", signature)).andExpect(status().isOk());
    }

    private String seatStatus(Fixture f, String label) throws Exception {
        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", f.showtimeId()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> statuses = JsonPath.read(body, "$.data.seats[?(@.id == " + f.seat(label) + ")].status");
        return statuses.get(0);
    }

    private ResultActions perform(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", session.bearer()));
    }

    private static Map<String, String> query(String url) {
        Map<String, String> params = new HashMap<>();
        UriComponentsBuilder.fromUriString(url)
                .build()
                .getQueryParams()
                .forEach((k, v) -> params.put(k, URLDecoder.decode(v.get(0), StandardCharsets.UTF_8)));
        return params;
    }

    private static Map<String, String> withoutHash(Map<String, String> params) {
        Map<String, String> copy = new TreeMap<>(params);
        copy.remove("vnp_SecureHash");
        return copy;
    }

    /** Ký giống VNPay: tham số sắp theo tên, giá trị URL-encode, HMAC-SHA512. */
    private static String sign(Map<String, String> params) throws Exception {
        String data = new TreeMap<>(params).entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(VNPAY_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private static long longAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }
}
