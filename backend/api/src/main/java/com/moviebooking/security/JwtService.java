package com.moviebooking.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** Tạo và kiểm tra access token (JWT, HS256). */
@Component
public class JwtService {

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_PERMISSIONS = "perms";

    private final SecretKey key;
    private final Duration accessTokenTtl;
    private final String issuer;
    private final Clock clock;

    public JwtService(SecurityProperties properties, Clock clock) {
        SecurityProperties.Jwt jwt = properties.jwt();
        if (jwt == null || jwt.secret() == null || jwt.secret().isBlank()) {
            throw new IllegalStateException("app.security.jwt.secret must be configured");
        }
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
        this.accessTokenTtl = jwt.accessTokenTtl();
        this.issuer = jwt.issuer();
        this.clock = clock;
    }

    public IssuedToken issue(AuthenticatedUser user) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(accessTokenTtl);
        String token = Jwts.builder()
                .issuer(issuer)
                .subject(String.valueOf(user.id()))
                .claim(CLAIM_EMAIL, user.email())
                .claim(CLAIM_NAME, user.fullName())
                .claim(CLAIM_ROLES, List.copyOf(user.roles()))
                .claim(CLAIM_PERMISSIONS, List.copyOf(user.permissions()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    /**
     * @throws JwtException nếu token sai chữ ký, sai issuer, hết hạn hoặc không đúng định dạng
     */
    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new AuthenticatedUser(
                Long.valueOf(claims.getSubject()),
                claims.get(CLAIM_EMAIL, String.class),
                claims.get(CLAIM_NAME, String.class),
                toSet(claims.get(CLAIM_ROLES, Collection.class)),
                toSet(claims.get(CLAIM_PERMISSIONS, Collection.class)));
    }

    private static Set<String> toSet(Collection<?> values) {
        Set<String> result = new LinkedHashSet<>();
        if (values != null) {
            values.forEach(value -> result.add(String.valueOf(value)));
        }
        return Set.copyOf(result);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
