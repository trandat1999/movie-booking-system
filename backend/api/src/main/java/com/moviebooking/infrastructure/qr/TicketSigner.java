package com.moviebooking.infrastructure.qr;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Nội dung mã QR của vé: {@code <mã vé>.<HMAC-SHA256 rút gọn>} (BR07). Không làm giả được mã QR nếu không có
 * khóa; khi check-in (M7) vẫn đối chiếu CSDL để vé chỉ dùng được một lần.
 */
@Component
public class TicketSigner {

    private static final int SIGNATURE_BYTES = 16;

    private final byte[] secret;

    public TicketSigner(@Value("${app.ticket.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.ticket.secret must be configured");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String sign(String ticketCode) {
        return ticketCode + "." + signature(ticketCode);
    }

    /** @return mã vé nếu chữ ký đúng */
    public Optional<String> verify(String payload) {
        int dot = payload == null ? -1 : payload.lastIndexOf('.');
        if (dot <= 0) {
            return Optional.empty();
        }
        String code = payload.substring(0, dot);
        byte[] expected = signature(code).getBytes(StandardCharsets.US_ASCII);
        byte[] actual = payload.substring(dot + 1).getBytes(StandardCharsets.US_ASCII);
        return MessageDigest.isEqual(expected, actual) ? Optional.of(code) : Optional.empty();
    }

    private String signature(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(Arrays.copyOf(digest, SIGNATURE_BYTES));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC not available", ex);
        }
    }
}
