package com.moviebooking.modules.user.event;

/**
 * Phát ra khi mọi phiên đăng nhập của người dùng phải chấm dứt (đổi mật khẩu, bị khóa). Module auth lắng nghe
 * đồng bộ, trong cùng transaction, để thu hồi refresh token.
 */
public record UserSessionsInvalidatedEvent(Long userId) {}
