package com.moviebooking.infrastructure.payment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình VNPay, tiền tố {@code app.payment.vnpay.*}. Để trống mã merchant/khóa bí mật thì VNPay tắt (dev/test
 * dùng cổng giả lập).
 *
 * @param tmnCode mã website (vnp_TmnCode) do VNPay cấp
 * @param hashSecret khóa bí mật ký HMAC-SHA512
 * @param payUrl URL cổng thanh toán (mặc định sandbox)
 * @param returnUrl trang web khách hàng nhận kết quả sau khi thanh toán
 * @param expireMinutes thời hạn phiên thanh toán
 */
@ConfigurationProperties(prefix = "app.payment.vnpay")
public record VnpayProperties(
        @DefaultValue("") String tmnCode,
        @DefaultValue("") String hashSecret,
        @DefaultValue("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html") String payUrl,
        @DefaultValue("http://localhost:4200/payment/vnpay-return") String returnUrl,
        @DefaultValue("15") int expireMinutes) {

    public boolean configured() {
        return !tmnCode.isBlank() && !hashSecret.isBlank();
    }
}
