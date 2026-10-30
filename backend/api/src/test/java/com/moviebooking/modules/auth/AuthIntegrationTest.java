package com.moviebooking.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moviebooking.modules.audit.repository.AuditLogRepository;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.enums.UserStatus;
import com.moviebooking.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void registerCreatesCustomerAndStartsSession() throws Exception {
        String email = uniqueEmail("Reg").toUpperCase();
        String phone = randomPhone();

        MvcResult result = register(email, phone, PASSWORD)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.data.user.roles[0]").value(Role.CUSTOMER))
                .andExpect(cookie().httpOnly(REFRESH_COOKIE, true))
                .andExpect(cookie().path(REFRESH_COOKIE, "/api/v1/auth"))
                .andReturn();
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");

        Session session = Session.from(result);
        mockMvc.perform(get("/api/v1/me").header("Authorization", session.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email.toLowerCase()))
                .andExpect(jsonPath("$.data.phone").value(phone));
    }

    @Test
    void registerRejectsDuplicateEmailAndPhone() throws Exception {
        String email = uniqueEmail("dup");
        String phone = randomPhone();
        register(email, phone, PASSWORD).andExpect(status().isCreated());

        register(email, null, PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
        register(uniqueEmail("dup2"), "+84" + phone.substring(1), PASSWORD)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("PHONE_ALREADY_EXISTS"));
    }

    @Test
    void registerValidatesInput() throws Exception {
        register("not-an-email", "123", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details[*].field", containsInAnyOrder("email", "phone", "password")));
    }

    @Test
    void loginWorksWithEmailOrPhone() throws Exception {
        String email = uniqueEmail("login");
        String phone = randomPhone();
        register(email, phone, PASSWORD).andExpect(status().isCreated());

        assertThat(login(email.toUpperCase(), PASSWORD).accessToken()).isNotBlank();
        assertThat(login(phone, PASSWORD).accessToken()).isNotBlank();
    }

    @Test
    void wrongPasswordIsRejectedAndAudited() throws Exception {
        User user = createUser(uniqueEmail("wrong"), Role.CUSTOMER);
        long before = auditLogRepository.count();

        loginRequest(user.getEmail(), "Wrong12345")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        loginRequest(uniqueEmail("nobody"), PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));

        assertThat(auditLogRepository.count()).isEqualTo(before + 2);
    }

    @Test
    void lockedUserCannotLogin() throws Exception {
        User user = createUser(uniqueEmail("locked"), Role.CUSTOMER);
        user.setStatus(UserStatus.LOCKED);
        userRepository.save(user);

        loginRequest(user.getEmail(), PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void protectedApiRequiresValidAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());

        Session session = login(createUser(uniqueEmail("exp"), Role.CUSTOMER).getEmail(), PASSWORD);
        clock.advance(Duration.ofMinutes(16));
        mockMvc.perform(get("/api/v1/me").header("Authorization", session.bearer()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshRotatesTokenAndDetectsReuse() throws Exception {
        Session first = login(createUser(uniqueEmail("rot"), Role.CUSTOMER).getEmail(), PASSWORD);

        Session second = Session.from(
                refresh(first.refreshCookie()).andExpect(status().isOk()).andReturn());
        assertThat(second.refreshCookie().getValue()).isNotEqualTo(first.refreshCookie().getValue());
        mockMvc.perform(get("/api/v1/me").header("Authorization", second.bearer())).andExpect(status().isOk());

        // Trong thời gian ân hạn (tab khác refresh cùng lúc) vẫn được chấp nhận
        refresh(first.refreshCookie()).andExpect(status().isOk());

        // Quá thời gian ân hạn: coi là bị đánh cắp → thu hồi cả chuỗi, kể cả token mới nhất
        clock.advance(Duration.ofSeconds(31));
        refresh(first.refreshCookie())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"))
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));
        refresh(second.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithoutCookieIsRejected() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutRevokesSession() throws Exception {
        Session session = login(createUser(uniqueEmail("logout"), Role.CUSTOMER).getEmail(), PASSWORD);

        mockMvc.perform(post("/api/v1/auth/logout").cookie(session.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(REFRESH_COOKIE, 0));

        refresh(session.refreshCookie()).andExpect(status().isUnauthorized());
    }

    @Test
    void updateProfileNormalizesPhone() throws Exception {
        Session session = login(createUser(uniqueEmail("profile"), Role.CUSTOMER).getEmail(), PASSWORD);
        String phone = randomPhone();

        mockMvc.perform(patch("/api/v1/me")
                        .header("Authorization", session.bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("fullName", "  Trần Thị B  ", "phone", "+84" + phone.substring(1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Trần Thị B"))
                .andExpect(jsonPath("$.data.phone").value(phone));
    }

    @Test
    void changePasswordRevokesAllSessions() throws Exception {
        User user = createUser(uniqueEmail("pwd"), Role.CUSTOMER);
        Session session = login(user.getEmail(), PASSWORD);

        changePassword(session, "Wrong12345", "NewPass456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_CURRENT_PASSWORD"));
        changePassword(session, PASSWORD, "NewPass456").andExpect(status().isOk());

        refresh(session.refreshCookie()).andExpect(status().isUnauthorized());
        loginRequest(user.getEmail(), PASSWORD).andExpect(status().isUnauthorized());
        assertThat(login(user.getEmail(), "NewPass456").accessToken()).isNotBlank();
    }

    private ResultActions register(String email, String phone, String password) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", "Người Dùng Test");
        body.put("email", email);
        body.put("phone", phone);
        body.put("password", password);
        return mockMvc.perform(
                post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(json(body)));
    }

    private ResultActions loginRequest(String identifier, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("identifier", identifier, "password", password))));
    }

    private ResultActions refresh(Cookie cookie) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/refresh").cookie(cookie));
    }

    private ResultActions changePassword(Session session, String current, String next) throws Exception {
        return mockMvc.perform(post("/api/v1/me/password")
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("currentPassword", current, "newPassword", next))));
    }

    private static String randomPhone() {
        return "09" + String.format("%08d", (long) (Math.random() * 100_000_000L));
    }
}
