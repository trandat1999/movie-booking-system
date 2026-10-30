package com.moviebooking.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientInfoTest {

    @Test
    void ignoresClientSuppliedForwardedForHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("X-Forwarded-For", "1.2.3.4");

        // Header tự đặt không được dùng để giả IP (vượt rate limit đăng nhập theo IP)
        assertThat(ClientInfo.from(request).ipAddress()).isEqualTo("203.0.113.7");
    }

    @Test
    void truncatesLongUserAgent() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("User-Agent", "x".repeat(1000));

        assertThat(ClientInfo.from(request).userAgent()).hasSize(255);
    }
}
