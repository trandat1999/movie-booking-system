package com.moviebooking.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.moviebooking.support.MutableClock;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dGVzdC1vbmx5LXNlY3JldC1rZXktZm9yLWp3dC1zaWduaW5nLW1vdmllLWJvb2tpbmctMjAyNg==";
    private static final String OTHER_SECRET =
            "b3RoZXItc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmctbW92aWUtYm9va2luZy0yMDI2LXh4eA==";

    private MutableClock clock;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-30T08:00:00Z"));
        jwtService = new JwtService(properties(SECRET), clock);
    }

    @Test
    void issuedTokenRoundTripsUserAndAuthorities() {
        AuthenticatedUser user =
                new AuthenticatedUser(42L, "a@b.vn", "Nguyễn Văn A", Set.of("ADMIN"), Set.of("user:read", "admin:access"));

        JwtService.IssuedToken token = jwtService.issue(user);
        AuthenticatedUser parsed = jwtService.parse(token.value());

        assertThat(token.expiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(15)));
        assertThat(parsed).isEqualTo(user);
        assertThat(parsed.authorities())
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "user:read", "admin:access");
    }

    @Test
    void expiredTokenIsRejected() {
        String token = jwtService.issue(customer()).value();
        clock.advance(Duration.ofMinutes(16));

        assertThatThrownBy(() -> jwtService.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String foreign = new JwtService(properties(OTHER_SECRET), clock).issue(customer()).value();

        assertThatThrownBy(() -> jwtService.parse(foreign)).isInstanceOf(JwtException.class);
    }

    @Test
    void tamperedPayloadIsRejected() {
        String[] parts = jwtService.issue(customer()).value().split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "xx." + parts[2];

        assertThatThrownBy(() -> jwtService.parse(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void missingSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService(properties(""), clock)).isInstanceOf(IllegalStateException.class);
    }

    private static AuthenticatedUser customer() {
        return new AuthenticatedUser(1L, "c@d.vn", "Khách", Set.of("CUSTOMER"), Set.of());
    }

    private static SecurityProperties properties(String secret) {
        return new SecurityProperties(
                new SecurityProperties.Jwt(secret, Duration.ofMinutes(15), "movie-booking"),
                Duration.ofDays(7),
                Duration.ofSeconds(30),
                "mb_refresh",
                false);
    }
}
