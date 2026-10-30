package com.moviebooking.modules.auth.service.impl;

import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.audit.dto.request.AuditEntry;
import com.moviebooking.modules.audit.service.AuditService;
import com.moviebooking.modules.auth.entity.RefreshToken;
import com.moviebooking.modules.auth.enums.RevokeReason;
import com.moviebooking.modules.auth.repository.RefreshTokenRepository;
import com.moviebooking.modules.auth.service.RefreshTokenService;
import com.moviebooking.security.SecurityProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecurityProperties securityProperties;
    private final AuditService auditService;
    private final Clock clock;

    @Override
    @Transactional
    public String startSession(Long userId, ClientInfo client) {
        return issue(userId, UUID.randomUUID(), client);
    }

    /**
     * Không rollback khi ném {@link BusinessException}: việc thu hồi cả family lúc phát hiện dùng lại phải được
     * lưu dù request bị từ chối.
     */
    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public Rotation rotate(String rawToken, ClientInfo client) {
        RefreshToken token = findByRaw(rawToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        Instant now = clock.instant();

        if (token.isRevoked()) {
            if (!isWithinReuseGrace(token, now)) {
                refreshTokenRepository.revokeFamily(token.getFamilyId(), now, RevokeReason.REUSE_DETECTED);
                log.warn("Refresh token reuse detected for user {}, family revoked", token.getUserId());
                auditService.record(new AuditEntry(
                        "REFRESH_TOKEN_REUSE",
                        "user",
                        String.valueOf(token.getUserId()),
                        Map.of("familyId", token.getFamilyId().toString()),
                        token.getUserId(),
                        null,
                        client.ipAddress()));
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
            // Request song song trong thời gian ân hạn (nhiều tab cùng refresh): cấp thêm token cùng family,
            // miễn là phiên chưa bị đăng xuất/thu hồi.
            if (!refreshTokenRepository.existsByFamilyIdAndRevokedAtIsNull(token.getFamilyId())) {
                throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
        } else if (!token.getExpiresAt().isAfter(now)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        } else {
            token.revoke(now, RevokeReason.ROTATED);
        }
        return new Rotation(token.getUserId(), issue(token.getUserId(), token.getFamilyId(), client));
    }

    @Override
    @Transactional
    public void revokeSession(String rawToken) {
        findByRaw(rawToken)
                .filter(token -> !token.isRevoked())
                .ifPresent(token ->
                        refreshTokenRepository.revokeFamily(token.getFamilyId(), clock.instant(), RevokeReason.LOGOUT));
    }

    @Override
    @Transactional
    public void revokeAllForUser(Long userId, RevokeReason reason) {
        refreshTokenRepository.revokeAllByUserId(userId, clock.instant(), reason);
    }

    private boolean isWithinReuseGrace(RefreshToken token, Instant now) {
        return token.getRevokeReason() == RevokeReason.ROTATED
                && token.getRevokedAt().plus(securityProperties.refreshTokenReuseGrace()).isAfter(now);
    }

    private String issue(Long userId, UUID familyId, ClientInfo client) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(hash(raw));
        token.setFamilyId(familyId);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(securityProperties.refreshTokenTtl()));
        token.setUserAgent(client.userAgent());
        token.setIpAddress(client.ipAddress());
        refreshTokenRepository.save(token);
        return raw;
    }

    private Optional<RefreshToken> findByRaw(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        return refreshTokenRepository.findByTokenHash(hash(rawToken));
    }

    private static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
