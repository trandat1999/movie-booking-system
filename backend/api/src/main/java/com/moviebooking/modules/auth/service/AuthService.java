package com.moviebooking.modules.auth.service;

import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.modules.auth.dto.request.LoginRequest;
import com.moviebooking.modules.auth.dto.request.RegisterRequest;

public interface AuthService {

    /** Đăng ký tài khoản khách hàng và đăng nhập luôn. */
    AuthResult register(RegisterRequest request, ClientInfo client);

    AuthResult login(LoginRequest request, ClientInfo client);

    /** Đổi refresh token lấy access token mới; refresh token cũng được xoay vòng. */
    AuthResult refresh(String refreshToken, ClientInfo client);

    /** Thu hồi phiên đăng nhập của refresh token (bỏ qua nếu token không hợp lệ). */
    void logout(String refreshToken);
}
