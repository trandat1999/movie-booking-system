package com.moviebooking.modules.auth.service;

import com.moviebooking.modules.auth.dto.response.AuthResponse;

/** Kết quả nội bộ của service: body trả cho client và refresh token gốc để controller đặt vào cookie. */
public record AuthResult(AuthResponse response, String refreshToken) {}
