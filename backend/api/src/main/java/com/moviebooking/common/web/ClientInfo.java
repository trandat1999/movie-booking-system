package com.moviebooking.common.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Thông tin máy khách của request (IP, User-Agent) để lưu vào refresh token và nhật ký. */
public record ClientInfo(String ipAddress, String userAgent) {

    private static final int MAX_USER_AGENT_LENGTH = 255;
    private static final ClientInfo UNKNOWN = new ClientInfo(null, null);

    /**
     * IP lấy từ {@code getRemoteAddr()}. KHÔNG đọc trực tiếp {@code X-Forwarded-For}: máy khách tự đặt được header
     * này để giả IP (vượt giới hạn đăng nhập theo IP, ghi sai nhật ký). Khi chạy sau reverse proxy, Tomcat
     * ({@code server.forward-headers-strategy=native}) chỉ chấp nhận header từ proxy nội bộ tin cậy và tự thay
     * remote address.
     */
    public static ClientInfo from(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        if (userAgent != null && userAgent.length() > MAX_USER_AGENT_LENGTH) {
            userAgent = userAgent.substring(0, MAX_USER_AGENT_LENGTH);
        }
        return new ClientInfo(ip, userAgent);
    }

    /** Lấy từ request hiện tại nếu đang trong một HTTP request, ngược lại trả về giá trị rỗng. */
    public static ClientInfo current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return from(attributes.getRequest());
        }
        return UNKNOWN;
    }
}
