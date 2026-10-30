package com.moviebooking.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.moviebooking.modules.booking.service.HoldService;
import com.moviebooking.modules.group.service.GroupService;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.util.UriComponentsBuilder;

/** Đặt vé theo nhóm end-to-end (M6): tạo → mời → phản hồi → bình chọn → chốt → đề xuất ghế → giữ → thanh toán. */
class GroupIntegrationTest extends AbstractAdminIntegrationTest {

    @Autowired
    private HoldService holdService;

    @Autowired
    private GroupService groupService;

    private Session leader;
    private Session alice;
    private Session bob;
    private Fixture fixture;

    @BeforeEach
    void setUpGroupMembers() throws Exception {
        leader = customer();
        alice = customer();
        bob = customer();
        fixture = bookableShowtime();
    }

    @Test
    void wholeGroupFlowFromInviteToTickets() throws Exception {
        long groupId = createGroup(leader, fixture);
        String code = string(perform(leader, get("/api/v1/groups/{id}", groupId)).andReturn(), "$.data.code");

        perform(alice, get("/api/v1/groups/invite/{code}", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyMember").value(false));
        join(alice, code).andExpect(status().isOk()).andExpect(jsonPath("$.data.myRole").value("MEMBER"));
        join(bob, code).andExpect(status().isOk());
        join(bob, code).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("ALREADY_GROUP_MEMBER"));

        rsvp(alice, groupId, "YES", 1).andExpect(status().isOk());
        rsvp(bob, groupId, "NO", 0).andExpect(status().isOk());

        MvcResult proposed = perform(alice, post("/api/v1/groups/{id}/options", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", fixture.showtimeId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options[0].votes").value(1))
                .andReturn();
        long optionId = longAt(proposed, "$.data.options[0].id");
        perform(leader, post("/api/v1/groups/{id}/options/{o}/vote", groupId, optionId))
                .andExpect(jsonPath("$.data.options[0].votes").value(2))
                .andExpect(jsonPath("$.data.options[0].votedByMe").value(true));

        perform(bob, get("/api/v1/groups/{id}/summary", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketCount").value(3))
                .andExpect(jsonPath("$.data.yes").value(2))
                .andExpect(jsonPath("$.data.no").value(1))
                .andExpect(jsonPath("$.data.guests").value(1))
                .andExpect(jsonPath("$.data.topOptionId").value(optionId))
                .andExpect(jsonPath("$.data.options[0].cluster").value("SAME_ROW"));

        // Chỉ trưởng nhóm được chốt (BR08)
        lock(alice, groupId, null).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_GROUP_LEADER"));
        lock(leader, groupId, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"))
                .andExpect(jsonPath("$.data.locked.ticketCount").value(3));
        // Đã chốt thì không nhận thành viên mới
        join(customer(), code).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("GROUP_NOT_OPEN"));

        MvcResult suggestions = suggest(leader, groupId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ticketCount").value(3))
                .andExpect(jsonPath("$.data.options[0].rank").value(1))
                .andExpect(jsonPath("$.data.options[0].rowCount").value(1))
                .andReturn();
        List<Integer> seatIds = JsonPath.read(suggestions.getResponse().getContentAsString(), "$.data.options[0].seatIds");
        long logId = longAt(suggestions, "$.data.logId");

        perform(leader, post("/api/v1/groups/{id}/preview", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("seatIds", seatIds))))
                .andExpect(status().isOk());
        hold(leader, groupId, seatIds, logId, 1)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HOLDING"))
                .andExpect(jsonPath("$.data.hold.seatIds.length()").value(3));
        perform(alice, get("/api/v1/groups/{id}", groupId))
                .andExpect(jsonPath("$.data.status").value("HOLDING"))
                .andExpect(jsonPath("$.data.hold.seatIds.length()").value(3));

        MvcResult order = perform(leader, post("/api/v1/groups/{id}/order", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.total").value(3 * fixture.price()))
                .andReturn();
        String orderCode = string(order, "$.data.code");
        perform(alice, post("/api/v1/orders/{code}/pay", orderCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("provider", "MOCK"))))
                .andExpect(status().isNotFound());
        ipn(startPayment(leader, orderCode)).andExpect(jsonPath("$.data.orderStatus").value("PAID"));

        MvcResult booked = perform(alice, get("/api/v1/groups/{id}", groupId))
                .andExpect(jsonPath("$.data.status").value("BOOKED"))
                .andExpect(jsonPath("$.data.order.status").value("PAID"))
                .andExpect(jsonPath("$.data.assignments.length()").value(3))
                .andExpect(jsonPath("$.data.shares.length()").value(2))
                .andReturn();
        // Chia tiền tham khảo: trưởng nhóm 1 người, Alice 2 người (bản thân + khách)
        List<Integer> amounts = JsonPath.read(booked.getResponse().getContentAsString(), "$.data.shares[*].amount");
        assertThat(amounts.stream().mapToLong(Integer::longValue).sum()).isEqualTo(3 * fixture.price());

        // Vé được phân phối: trưởng nhóm 1 vé, Alice 2 vé (của mình + của khách), Bob không đi
        perform(leader, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(1));
        perform(alice, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(2));
        perform(bob, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(0));

        // Trưởng nhóm chuyển vé của khách sang Bob
        List<String> ticketCodes = JsonPath.read(booked.getResponse().getContentAsString(), "$.data.assignments[*].ticketCode");
        long bobId = userId(bob);
        perform(leader, put("/api/v1/groups/{id}/assignments", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("items", List.of(Map.of("ticketCode", ticketCodes.get(2), "userId", bobId))))))
                .andExpect(status().isOk());
        perform(bob, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(1));
        perform(alice, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(1));

        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(jsonPath("$.data.items[?(@.type == 'GROUP_PAID')]").exists())
                .andExpect(jsonPath("$.data.items[?(@.type == 'GROUP_LOCKED')]").exists());
        perform(alice, get("/api/v1/me/groups"))
                .andExpect(jsonPath("$.data[0].id").value(groupId))
                .andExpect(jsonPath("$.data[0].status").value("BOOKED"))
                .andExpect(jsonPath("$.data[0].memberCount").value(3));

        // Đơn nhóm chỉ hủy từ trang nhóm (GRP21); hủy → hoàn tiền, vé mọi người bị hủy
        perform(leader, post("/api/v1/orders/{code}/cancel", orderCode))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GROUP_ORDER_CANCEL_VIA_GROUP"));
        perform(leader, post("/api/v1/groups/{id}/cancel", groupId))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.order.status").value("REFUNDED"));
        perform(bob, get("/api/v1/me/tickets")).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void cancelledShowtimeSendsLockedGroupBackToPlanning() throws Exception {
        long groupId = lockedGroup(2);
        postJson("/api/v1/admin/showtimes/{id}/cancel", Map.of(), fixture.showtimeId()).andExpect(status().isOk());
        perform(alice, get("/api/v1/groups/{id}", groupId))
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.locked").doesNotExist());
        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(jsonPath("$.data.items[?(@.type == 'GROUP_SHOWTIME_CANCELLED')]").exists());
    }

    @Test
    void outsidersCannotSeeOrChangeGroup() throws Exception {
        long groupId = createGroup(leader, fixture);
        perform(alice, get("/api/v1/groups/{id}", groupId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("GROUP_NOT_FOUND"));
        rsvp(alice, groupId, "YES", 0).andExpect(status().isNotFound());
        join(alice, "NOPE1234").andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVITE_CODE_INVALID"));
        assertThat(groupService.isActiveMember(groupId, userId(alice))).isFalse();
        assertThat(groupService.isActiveMember(groupId, userId(leader))).isTrue();
    }

    @Test
    void failedGroupHoldReturnsFreshSuggestionsAndKeepsNothing() throws Exception {
        long groupId = lockedGroup(4);
        List<Integer> best = JsonPath.read(
                suggest(leader, groupId).andReturn().getResponse().getContentAsString(), "$.data.options[0].seatIds");

        // Một khách lẻ giữ mất 2 ghế giữa phương án tốt nhất
        perform(bob, post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", fixture.showtimeId(), "seatIds", best.subList(1, 3)))))
                .andExpect(status().isOk());

        MvcResult failed = hold(leader, groupId, best, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SEAT_UNAVAILABLE"))
                .andExpect(jsonPath("$.error.details.seatIds.length()").value(2))
                .andExpect(jsonPath("$.error.details.suggestions.options[0].rank").value(1))
                .andReturn();
        List<Integer> fresh = JsonPath.read(
                failed.getResponse().getContentAsString(), "$.error.details.suggestions.options[*].seatIds[*]");
        assertThat(fresh).doesNotContainAnyElementsOf(best.subList(1, 3));
        // Không giữ một phần (BR03)
        assertThat(seatStatus(best.get(0).longValue())).isEqualTo("AVAILABLE");
        perform(leader, get("/api/v1/groups/{id}", groupId)).andExpect(jsonPath("$.data.status").value("LOCKED"));
    }

    @Test
    void seatCountMustMatchTicketCount() throws Exception {
        long groupId = lockedGroup(3);
        hold(leader, groupId, List.of(fixture.seat("A1").intValue(), fixture.seat("A2").intValue()), null, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SEAT_COUNT_MISMATCH"));
        perform(leader, post("/api/v1/groups/{id}/seat-evaluation", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("seatIds", List.of(fixture.seat("A1"), fixture.seat("A2"), fixture.seat("A3"))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rank").value(0))
                .andExpect(jsonPath("$.data.capacity").value(3));
    }

    @Test
    void expiredGroupHoldReturnsGroupToLocked() throws Exception {
        long groupId = lockedGroup(2);
        List<Integer> best = JsonPath.read(
                suggest(leader, groupId).andReturn().getResponse().getContentAsString(), "$.data.options[0].seatIds");
        MvcResult held = hold(leader, groupId, best, null, null).andExpect(status().isOk()).andReturn();
        long holdId = longAt(held, "$.data.hold.holdId");

        // Lượt giữ cá nhân của trưởng nhóm cho cùng suất không thay thế lượt giữ của nhóm
        perform(leader, post("/api/v1/holds")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", fixture.showtimeId(),
                                "seatIds", List.of(fixture.seat("D1"), fixture.seat("D2"))))))
                .andExpect(status().isOk());
        perform(leader, get("/api/v1/groups/{id}", groupId)).andExpect(jsonPath("$.data.status").value("HOLDING"));

        clock.advance(Duration.ofMinutes(11));
        assertThat(holdService.expire(holdId)).isTrue();
        perform(alice, get("/api/v1/groups/{id}", groupId))
                .andExpect(jsonPath("$.data.status").value("LOCKED"))
                .andExpect(jsonPath("$.data.hold").doesNotExist());
        assertThat(seatStatus(best.get(0).longValue())).isEqualTo("AVAILABLE");
    }

    @Test
    void leaderReleasesHoldAndCancelsGroup() throws Exception {
        long groupId = lockedGroup(2);
        List<Integer> best = JsonPath.read(
                suggest(leader, groupId).andReturn().getResponse().getContentAsString(), "$.data.options[0].seatIds");
        hold(leader, groupId, best, null, null).andExpect(status().isOk());
        perform(leader, delete("/api/v1/groups/{id}/hold", groupId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));
        assertThat(seatStatus(best.get(0).longValue())).isEqualTo("AVAILABLE");

        perform(leader, post("/api/v1/groups/{id}/unlock", groupId)).andExpect(jsonPath("$.data.status").value("OPEN"));
        perform(leader, post("/api/v1/groups/{id}/cancel", groupId)).andExpect(jsonPath("$.data.status").value("CANCELLED"));
        rsvp(alice, groupId, "YES", 0).andExpect(status().isConflict());
    }

    @Test
    void leaderMustTransferBeforeLeaving() throws Exception {
        long groupId = createGroup(leader, fixture);
        join(alice, codeOf(groupId)).andExpect(status().isOk());
        perform(leader, post("/api/v1/groups/{id}/leave", groupId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("LEADER_CANNOT_LEAVE"));
        perform(leader, post("/api/v1/groups/{id}/transfer-leader", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("userId", userId(alice)))))
                .andExpect(jsonPath("$.data.leaderId").value(userId(alice)));
        perform(leader, post("/api/v1/groups/{id}/leave", groupId)).andExpect(status().isOk());
        perform(leader, get("/api/v1/groups/{id}", groupId)).andExpect(status().isNotFound());
        perform(alice, get("/api/v1/groups/{id}", groupId))
                .andExpect(jsonPath("$.data.members.length()").value(1))
                .andExpect(jsonPath("$.data.myRole").value("LEADER"));
    }

    @Test
    void groupSizeAndOptionRulesAreEnforced() throws Exception {
        long groupId = createGroup(leader, fixture);
        join(alice, codeOf(groupId)).andExpect(status().isOk());
        // group_max_size = 20: trưởng nhóm + Alice + 19 khách = 21
        rsvp(alice, groupId, "YES", 19).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("GROUP_FULL"));

        // Suất của phim khác không được đề xuất
        Fixture other = bookableShowtime();
        perform(alice, post("/api/v1/groups/{id}/options", groupId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("showtimeId", other.showtimeId()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("SHOWTIME_NOT_ELIGIBLE"));
        lock(leader, groupId, 21).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_TICKET_COUNT"));
    }

    @Test
    void underAgeMembersAreWarned() throws Exception {
        long groupId = createGroup(leader, fixture);
        join(alice, codeOf(groupId)).andExpect(status().isOk());
        User aliceUser = userRepository.findById(userId(alice)).orElseThrow();
        aliceUser.setDateOfBirth(LocalDate.now().minusYears(10));
        userRepository.save(aliceUser);
        // Phim của fixture phân loại T13
        perform(leader, get("/api/v1/groups/{id}/summary", groupId))
                .andExpect(jsonPath("$.data.ageWarnings.length()").value(1));
    }

    @Test
    void deadlineRemindsPendingMembersThenExpiresGroup() throws Exception {
        long groupId = createGroup(leader, fixture, clock.instant().plus(Duration.ofMinutes(30)));
        join(alice, codeOf(groupId)).andExpect(status().isOk());

        assertThat(groupService.remindPendingMembers()).isGreaterThanOrEqualTo(1);
        perform(alice, get("/api/v1/me/notifications"))
                .andExpect(jsonPath("$.data.items[?(@.type == 'GROUP_RSVP_REMINDER')]").exists());

        clock.advance(Duration.ofMinutes(31));
        assertThat(groupService.expireOverdueGroups()).isGreaterThanOrEqualTo(1);
        Session fresh = login(email(alice), PASSWORD);
        perform(fresh, get("/api/v1/groups/{id}", groupId)).andExpect(jsonPath("$.data.status").value("EXPIRED"));
    }

    // --- helpers ---

    private record Fixture(long showtimeId, long movieId, Map<String, Long> seats, long price) {
        Long seat(String label) {
            return seats.get(label);
        }
    }

    private final Map<Session, String> emails = new HashMap<>();

    private Session customer() throws Exception {
        String email = uniqueEmail("customer");
        createUser(email, Role.CUSTOMER);
        Session session = login(email, PASSWORD);
        emails.put(session, email);
        return session;
    }

    private String email(Session session) {
        return emails.get(session);
    }

    private long userId(Session session) {
        return userRepository.findWithRolesByEmail(emails.get(session)).orElseThrow().getId();
    }

    /** Suất 3 ngày sau ở phòng mới: 4 hàng × 8 ghế thường liền nhau (A1–D8). */
    private Fixture bookableShowtime() throws Exception {
        long roomId = createRoom(createCinema());
        List<Map<String, Object>> layout = new ArrayList<>();
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 8; col++) {
                layout.add(Map.of("gridRow", row, "gridCol", col, "seatTypeCode", "STANDARD"));
            }
        }
        putJson("/api/v1/admin/rooms/{id}/layout", Map.of("gridRows", 4, "gridCols", 8, "seats", layout), roomId)
                .andExpect(status().isOk());
        long movieId = createMovie(100);
        Instant startAt = clock.instant().plus(Duration.ofDays(3)).truncatedTo(ChronoUnit.HOURS);
        long showtimeId = idOf(createShowtime(movieId, roomId, startAt).andExpect(status().isCreated()).andReturn());
        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", showtimeId))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Map<String, Long> seats = new HashMap<>();
        List<Map<String, Object>> list = JsonPath.read(body, "$.data.seats");
        list.forEach(s -> seats.put(s.get("rowLabel") + String.valueOf(s.get("number")), ((Number) s.get("id")).longValue()));
        long price = ((Number) JsonPath.read(body, "$.data.seatTypes[0].price")).longValue();
        return new Fixture(showtimeId, movieId, seats, price);
    }

    private long createGroup(Session session, Fixture f) throws Exception {
        return createGroup(session, f, clock.instant().plus(Duration.ofDays(2)));
    }

    private long createGroup(Session session, Fixture f, Instant deadline) throws Exception {
        MvcResult result = perform(session, post("/api/v1/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Nhóm xem phim", "movieId", f.movieId(), "rsvpDeadline", deadline.toString()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.myRole").value("LEADER"))
                .andReturn();
        return longAt(result, "$.data.id");
    }

    /** Nhóm đã chốt suất của fixture với {@code tickets} vé. */
    private long lockedGroup(int tickets) throws Exception {
        long groupId = createGroup(leader, fixture);
        join(alice, codeOf(groupId)).andExpect(status().isOk());
        lock(leader, groupId, tickets).andExpect(status().isOk());
        return groupId;
    }

    private String codeOf(long groupId) throws Exception {
        return string(perform(leader, get("/api/v1/groups/{id}", groupId)).andReturn(), "$.data.code");
    }

    private ResultActions join(Session session, String code) throws Exception {
        return perform(session, post("/api/v1/groups/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("code", code))));
    }

    private ResultActions rsvp(Session session, long groupId, String rsvp, int guests) throws Exception {
        return perform(session, put("/api/v1/groups/{id}/rsvp", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("rsvp", rsvp, "guestCount", guests))));
    }

    private ResultActions lock(Session session, long groupId, Integer tickets) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("showtimeId", fixture.showtimeId());
        body.put("ticketCount", tickets);
        return perform(session, post("/api/v1/groups/{id}/lock", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private ResultActions suggest(Session session, long groupId) throws Exception {
        return perform(session, post("/api/v1/groups/{id}/seat-suggestions", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("allowSplit", true))));
    }

    private ResultActions hold(Session session, long groupId, List<Integer> seatIds, Long logId, Integer rank)
            throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("seatIds", seatIds);
        body.put("suggestionLogId", logId);
        body.put("chosenRank", rank);
        return perform(session, post("/api/v1/groups/{id}/hold", groupId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    private String seatStatus(long seatId) throws Exception {
        String body = mockMvc.perform(get("/api/v1/showtimes/{id}/seats", fixture.showtimeId()))
                .andReturn()
                .getResponse()
                .getContentAsString();
        List<String> statuses = JsonPath.read(body, "$.data.seats[?(@.id == " + seatId + ")].status");
        return statuses.get(0);
    }

    private Map<String, String> startPayment(Session session, String code) throws Exception {
        MvcResult result = perform(session, post("/api/v1/orders/{code}/pay", code)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("provider", "MOCK"))))
                .andExpect(status().isOk())
                .andReturn();
        String url = string(result, "$.data.redirectUrl");
        Map<String, String> params = new HashMap<>();
        UriComponentsBuilder.fromUriString("http://gateway" + url)
                .build()
                .getQueryParams()
                .forEach((k, v) -> params.put(k, URLDecoder.decode(v.get(0), StandardCharsets.UTF_8)));
        return params;
    }

    private ResultActions ipn(Map<String, String> gateway) throws Exception {
        return mockMvc.perform(post("/api/v1/payments/MOCK/ipn")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of(
                        "orderCode", gateway.get("orderCode"),
                        "transactionId", gateway.get("transactionId"),
                        "amount", gateway.get("amount"),
                        "result", "SUCCESS",
                        "signature", gateway.get("sigSuccess")))));
    }

    private ResultActions perform(Session session, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header("Authorization", session.bearer()));
    }

    private static long longAt(MvcResult result, String path) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), path)).longValue();
    }

    private static String string(MvcResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }
}
