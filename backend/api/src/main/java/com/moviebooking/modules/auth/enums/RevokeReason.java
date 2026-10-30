package com.moviebooking.modules.auth.enums;

public enum RevokeReason {
    /** Đã đổi sang token mới khi refresh. */
    ROTATED,
    /** Người dùng đăng xuất. */
    LOGOUT,
    /** Token đã thu hồi bị dùng lại → nghi bị đánh cắp, thu hồi cả chuỗi. */
    REUSE_DETECTED,
    /** Đổi mật khẩu hoặc bị khóa tài khoản. */
    SESSIONS_INVALIDATED
}
