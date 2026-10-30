package com.moviebooking.modules.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.infrastructure.qr.TicketSigner;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** M10: phim yêu thích (MOV04), phim tương tự (MOV05), đánh giá phim sau khi đã soát vé (FBK01). */
class MovieExtrasIntegrationTest extends AbstractAdminIntegrationTest {

    @Autowired
    private TicketSigner ticketSigner;

    private Session alice;
    private String aliceEmail;

    @BeforeEach
    void loginAlice() throws Exception {
        aliceEmail = uniqueEmail("alice");
        alice = login(createUser(aliceEmail, Role.CUSTOMER).getEmail(), PASSWORD);
    }

    @Test
    void favoritesAreIdempotentAndPrivate() throws Exception {
        long first = createMovie(100);
        long second = createMovie(110);
        perform(alice, put("/api/v1/me/favorites/{id}", first)).andExpect(status().isOk());
        perform(alice, put("/api/v1/me/favorites/{id}", first)).andExpect(status().isOk());
        clock.advance(Duration.ofSeconds(1));
        perform(alice, put("/api/v1/me/favorites/{id}", second)).andExpect(status().isOk());
        perform(alice, put("/api/v1/me/favorites/{id}", 999_999)).andExpect(status().isNotFound());

        perform(alice, get("/api/v1/me/favorites"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(second));
        Session bob = login(createUser(uniqueEmail("bob"), Role.CUSTOMER).getEmail(), PASSWORD);
        perform(bob, get("/api/v1/me/favorites")).andExpect(jsonPath("$.data.length()").value(0));

        perform(alice, delete("/api/v1/me/favorites/{id}", second)).andExpect(status().isOk());
        assertThat(ids(perform(alice, get("/api/v1/me/favorites")), "$.data[*].id")).containsExactly(first);
        mockMvc.perform(get("/api/v1/me/favorites")).andExpect(status().isUnauthorized());
    }

    @Test
    void similarMoviesShareMostGenresFirst() throws Exception {
        long action = createGenre();
        long comedy = createGenre();
        long drama = createGenre();
        long base = movie(List.of(action, comedy));
        long both = movie(List.of(action, comedy, drama));
        long onlyAction = movie(List.of(action));
        movie(List.of(drama));
        long ended = idOf(postJson("/api/v1/admin/movies", movieBody(unique("Phim cũ"), 90, "ENDED", List.of(action, comedy)))
                .andExpect(status().isCreated())
                .andReturn());

        // Chung 2 thể loại trước, chung 1 sau; phim chỉ khác thể loại và phim đã ngừng chiếu không có
        assertThat(ids(mockMvc.perform(get("/api/v1/movies/{slug}/similar", slugOf(base))), "$.data[*].id"))
                .containsExactly(both, onlyAction)
                .doesNotContain(ended);
    }

    @Test
    void onlyViewersWithCheckedInTicketCanReview() throws Exception {
        Showing s = showing(Duration.ofHours(10));
        String slug = slugOf(s.movieId());
        Map<String, Object> review = Map.of("rating", 4, "comment", "  Phim hay, âm thanh tốt  ");

        // Chưa xem: không được viết
        perform(alice, get("/api/v1/movies/{id}/my-review", s.movieId()))
                .andExpect(jsonPath("$.data.eligible").value(false))
                .andExpect(jsonPath("$.data.review").doesNotExist());
        saveReview(alice, s.movieId(), review).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("REVIEW_NOT_ALLOWED"));

        // Mua vé và được soát vé → được viết
        List<String> tickets = paidTickets(s);
        saveReview(alice, s.movieId(), review).andExpect(status().isForbidden());
        clock.advance(Duration.ofHours(9));
        Session staff = login(createUser(uniqueEmail("staff"), Role.STAFF).getEmail(), PASSWORD);
        perform(staff, post("/api/v1/admin/checkin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("payload", ticketSigner.sign(tickets.get(0))))))
                .andExpect(status().isOk());
        alice = login(aliceEmail, PASSWORD);
        admin = login(createUser(uniqueEmail("admin"), Role.ADMIN).getEmail(), PASSWORD);

        saveReview(alice, s.movieId(), review)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4))
                .andExpect(jsonPath("$.data.comment").value("Phim hay, âm thanh tốt"));
        // Sửa lại: vẫn một đánh giá
        saveReview(alice, s.movieId(), Map.of("rating", 2)).andExpect(jsonPath("$.data.comment").doesNotExist());
        saveReview(alice, s.movieId(), Map.of("rating", 6)).andExpect(status().isBadRequest());

        long reviewId = ((Number) JsonPath.read(
                        mockMvc.perform(get("/api/v1/movies/{slug}/reviews", slug))
                                .andExpect(jsonPath("$.data.count").value(1))
                                .andExpect(jsonPath("$.data.average").value(2.0))
                                .andExpect(jsonPath("$.data.items.items[0].rating").value(2))
                                .andReturn()
                                .getResponse()
                                .getContentAsString(),
                        "$.data.items.items[0].id"))
                .longValue();

        assertThat(ids(mockMvc.perform(get("/api/v1/movies/{slug}/reviews", slug)), "$.data.distribution"))
                .containsExactly(0L, 1L, 0L, 0L, 0L);

        // Quản trị ẩn → không hiển thị, không tính điểm; khách hàng không gọi được API quản trị
        perform(alice, put("/api/v1/admin/reviews/{id}/visibility", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("hidden", true))))
                .andExpect(status().isForbidden());
        putJson("/api/v1/admin/reviews/{id}/visibility", Map.of("hidden", true), reviewId)
                .andExpect(jsonPath("$.data.hidden").value(true));
        mockMvc.perform(get("/api/v1/movies/{slug}/reviews", slug))
                .andExpect(jsonPath("$.data.count").value(0))
                .andExpect(jsonPath("$.data.average").value(0.0));

        perform(alice, delete("/api/v1/movies/{id}/my-review", s.movieId())).andExpect(status().isOk());
        perform(alice, get("/api/v1/movies/{id}/my-review", s.movieId()))
                .andExpect(jsonPath("$.data.eligible").value(true))
                .andExpect(jsonPath("$.data.review").doesNotExist());
    }

    private record Showing(long showtimeId, long movieId, Map<String, Long> seats) {}

    private long movie(List<Long> genreIds) throws Exception {
        return idOf(postJson("/api/v1/admin/movies", movieBody(unique("Phim"), 100, "NOW_SHOWING", genreIds))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String slugOf(long movieId) throws Exception {
        return JsonPath.read(
                mockMvc.perform(get("/api/v1/admin/movies/{id}", movieId).header("Authorization", admin.bearer()))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.slug");
    }

    private Showing showing(Duration startIn) throws Exception {
        long roomId = createRoom(createCinema());
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int col = 0; col < 4; col++) {
            layout.add(Map.of("gridRow", 0, "gridCol", col, "seatTypeCode", "STANDARD"));
        }
        putJson("/api/v1/admin/rooms/{id}/layout", Map.of("gridRows", 1, "gridCols", 4, "seats", layout), roomId)
                .andExpect(status().isOk());
        long movieId = createMovie(100);
        Instant startAt = clock.instant().plus(startIn).truncatedTo(ChronoUnit.MINUTES);
        long showtimeId = idOf(createShowtime(movieId, roomId, startAt).andExpect(status().isCreated()).andReturn());
        List<Map<String, Object>> list = JsonPath.read(
                mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId)).andReturn().getResponse().getContentAsString(),
                "$.data.seats");
        Map<String, Long> seats = new HashMap<>();
        list.forEach(x -> seats.put(x.get("rowLabel") + String.valueOf(x.get("number")), ((Number) x.get("id")).longValue()));
        return new Showing(showtimeId, movieId, seats);
    }

    /** Alice giữ A1, A2 → tạo đơn → thanh toán cổng giả lập; trả về mã vé. */
    private List<String> paidTickets(Showing s) throws Exception {
        String holdBody = perform(alice, post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", s.showtimeId(), "seatIds", List.of(s.seats().get("A1"), s.seats().get("A2"))))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        long holdId = ((Number) JsonPath.read(holdBody, "$.data.id")).longValue();
        String code = JsonPath.read(perform(alice, post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("holdId", holdId))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.code");
        String url = JsonPath.read(perform(alice, post("/api/v1/orders/{code}/pay", code)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("provider", "MOCK"))))
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$.data.redirectUrl");
        Map<String, String> p = UriComponentsBuilder.fromUriString("http://gateway" + url).build().getQueryParams().toSingleValueMap();
        mockMvc.perform(post("/api/v1/payments/MOCK/ipn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "orderCode", p.get("orderCode"),
                                "transactionId", p.get("transactionId"),
                                "amount", p.get("amount"),
                                "result", "SUCCESS",
                                "signature", java.net.URLDecoder.decode(p.get("sigSuccess"), java.nio.charset.StandardCharsets.UTF_8)))))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"));
        return JsonPath.read(
                perform(alice, get("/api/v1/orders/{code}", code)).andReturn().getResponse().getContentAsString(),
                "$.data.tickets[*].code");
    }

    private ResultActions saveReview(Session session, long movieId, Map<String, Object> body) throws Exception {
        return perform(session, put("/api/v1/movies/{id}/my-review", movieId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private static List<Long> ids(ResultActions result, String path) throws Exception {
        List<Number> values = JsonPath.read(result.andReturn().getResponse().getContentAsString(), path);
        return values.stream().map(Number::longValue).toList();
    }

    private ResultActions perform(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", session.bearer()));
    }
}
