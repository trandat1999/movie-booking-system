package com.moviebooking.infrastructure.payment;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Cổng thanh toán giả lập cho dev/test và demo (PLAN.md 8.4). Trang {@code /payment/mock} của web đóng vai cổng:
 * người dùng chọn thành công / thất bại, trang gửi "IPN" về backend kèm chữ ký HMAC do backend cấp sẵn cho từng
 * kết quả. Nhờ vậy client không thể tự tạo callback thành công giả.
 */
@Component
public class MockPaymentProvider implements PaymentProvider {

    public static final String CODE = "MOCK";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] secret;

    public MockPaymentProvider(@Value("${app.payment.mock.secret}") String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.payment.mock.secret must be configured");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public PaymentSession createPayment(String orderCode, long amount, String description) {
        byte[] random = new byte[6];
        RANDOM.nextBytes(random);
        String txnId = "MOCK" + HexFormat.of().formatHex(random).toUpperCase();
        String url = "/payment/mock?orderCode=" + orderCode
                + "&transactionId=" + txnId
                + "&amount=" + amount
                + "&description=" + URLEncoder.encode(description, StandardCharsets.UTF_8)
                + "&sigSuccess=" + sign(orderCode, txnId, amount, "SUCCESS")
                + "&sigFailed=" + sign(orderCode, txnId, amount, "FAILED");
        return new PaymentSession(url);
    }

    @Override
    public PaymentCallback verifyCallback(Map<String, String> params) {
        String orderCode = params.get("orderCode");
        String txnId = params.get("transactionId");
        String result = params.get("result");
        String signature = params.get("signature");
        long amount;
        try {
            amount = Long.parseLong(params.getOrDefault("amount", ""));
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }
        if (orderCode == null || txnId == null || result == null || signature == null
                || !MessageDigest.isEqual(
                        sign(orderCode, txnId, amount, result).getBytes(StandardCharsets.US_ASCII),
                        signature.getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }
        return new PaymentCallback(orderCode, txnId, amount, "SUCCESS".equals(result), Map.copyOf(params));
    }

    String sign(String orderCode, String txnId, long amount, String result) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            String data = orderCode + "|" + txnId + "|" + amount + "|" + result;
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC not available", ex);
        }
    }
}
