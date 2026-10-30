package com.moviebooking.modules.user.dto.request;

import java.time.LocalDate;

/** Dữ liệu tạo tài khoản khách hàng (do module auth gọi khi đăng ký). Email/SĐT chưa cần chuẩn hóa. */
public record CreateUserCommand(String email, String phone, String rawPassword, String fullName, LocalDate dateOfBirth) {}
