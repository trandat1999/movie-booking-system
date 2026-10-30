package com.moviebooking.infrastructure.payment;

import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * VNPay (API thanh toán 2.1.0, PLAN.md 8.4). Tham số được sắp theo tên, giá trị URL-encode, ký HMAC-SHA512 bằng
 * khóa bí mật. Kết quả về qua IPN (server–server, nguồn chính thức) và return URL (trình duyệt) — cả hai đều kiểm
 * tra chữ ký và xử lý idempotent theo {@code vnp_TxnRef}.
 *
 * <p>{@code vnp_TxnRef} = mã đơn (10 ký tự) + 6 chữ số ngẫu nhiên: VNPay yêu cầu mã giao dịch không trùng giữa các
 * lần thanh toán lại của cùng đơn.
 */
@Component
@RequiredArgsConstructor
public class VnpayProvider implements PaymentProvider {

    public static final String CODE = "VNPAY";
    private static final int SUFFIX_LENGTH = 6;
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(VNPAY_ZONE);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final VnpayProperties properties;
    private final Clock clock;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String displayName() {
        return "VNPay (thẻ ATM, QR, thẻ quốc tế)";
    }

    @Override
    public boolean enabled() {
        return properties.configured();
    }

    @Override
    public PaymentSession createPayment(String orderCode, long amount, String description, String clientIp) {
        if (!enabled()) {
            throw new BusinessException(ErrorCode.PAYMENT_PROVIDER_NOT_SUPPORTED);
        }
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.tmnCode());
        params.put("vnp_Amount", String.valueOf(amount * 100));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", orderCode + String.format("%06d", RANDOM.nextInt(1_000_000)));
        params.put("vnp_OrderInfo", description);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.returnUrl());
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", DATE.format(clock.instant()));
        params.put("vnp_ExpireDate", DATE.format(clock.instant().plusSeconds(properties.expireMinutes() * 60L)));
        String query = encode(params);
        return new PaymentSession(properties.payUrl() + "?" + query + "&vnp_SecureHash=" + sign(query));
    }

    @Override
    public PaymentCallback verifyCallback(Map<String, String> params) {
        String signature = params.get("vnp_SecureHash");
        String txnRef = params.get("vnp_TxnRef");
        if (!enabled() || signature == null || txnRef == null || txnRef.length() <= SUFFIX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }
        Map<String, String> signed = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !key.equals("vnp_SecureHash") && !key.equals("vnp_SecureHashType")
                    && value != null && !value.isEmpty()) {
                signed.put(key, value);
            }
        });
        String expected = sign(encode(signed));
        if (!MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                signature.toLowerCase(Locale.ROOT).getBytes(StandardCharsets.US_ASCII))) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }
        long amount;
        try {
            amount = Long.parseLong(params.getOrDefault("vnp_Amount", "")) / 100;
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_SIGNATURE);
        }
        boolean success = "00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"));
        String orderCode = txnRef.substring(0, txnRef.length() - SUFFIX_LENGTH);
        return new PaymentCallback(orderCode, txnRef, amount, success, Map.copyOf(params));
    }

    /** {@code key=value&...} theo thứ tự tên, giá trị URL-encode (đúng chuỗi VNPay dùng để ký). */
    static String encode(Map<String, String> sortedParams) {
        return sortedParams.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.US_ASCII) + "="
                        + URLEncoder.encode(e.getValue(), StandardCharsets.US_ASCII))
                .collect(Collectors.joining("&"));
    }

    String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(properties.hashSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("HMAC-SHA512 not available", ex);
        }
    }
}
