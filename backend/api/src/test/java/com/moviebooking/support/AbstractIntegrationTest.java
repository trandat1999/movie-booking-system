package com.moviebooking.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.repository.RoleRepository;
import com.moviebooking.modules.user.repository.UserRepository;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseType;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Lớp cha cho test tích hợp: khởi động toàn bộ ứng dụng với PostgreSQL 16 thật chạy nhúng (Zonky), không cần
 * Docker. Flyway chạy toàn bộ migration trên database này. Dữ liệu dùng chung giữa các test, nên mỗi test tự tạo
 * người dùng với email ngẫu nhiên.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(type = DatabaseType.POSTGRES, provider = DatabaseProvider.ZONKY)
@Import(AbstractIntegrationTest.TestClockConfig.class)
public abstract class AbstractIntegrationTest {

    protected static final String PASSWORD = "Secret123";
    protected static final String REFRESH_COOKIE = "mb_refresh";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MutableClock clock;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RoleRepository roleRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected static String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@test.local";
    }

    protected User createUser(String email, String... roleCodes) {
        User user = new User();
        user.setEmail(email);
        user.setFullName("Test " + email.substring(0, email.indexOf('@')));
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        for (String roleCode : roleCodes) {
            user.getRoles().add(roleRepository.findByCode(roleCode).orElseThrow());
        }
        return userRepository.save(user);
    }

    protected Session login(String identifier, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", identifier, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return Session.from(result);
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    /** Access token + cookie refresh token trả về sau đăng nhập/refresh. */
    public record Session(String accessToken, Cookie refreshCookie) {

        public static Session from(MvcResult result) throws Exception {
            String accessToken = JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
            return new Session(accessToken, result.getResponse().getCookie(REFRESH_COOKIE));
        }

        public String bearer() {
            return "Bearer " + accessToken;
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(Instant.now());
        }
    }
}
