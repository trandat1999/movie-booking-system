package com.moviebooking.modules.user.dto.response;

/** Thông tin ngắn của người dùng cho màn quản trị module khác (đơn, nhóm). */
public record UserBrief(Long id, String fullName, String email, String phone) {}
