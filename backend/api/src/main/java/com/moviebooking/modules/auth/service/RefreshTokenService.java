package com.moviebooking.modules.auth.service;

import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.modules.auth.enums.RevokeReason;

/**
 * Quản lý refresh token theo cơ chế xoay vòng (rotation) + phát hiện dùng lại (reuse detection):
 *
 * <ul>
 *   <li>Mỗi lần đăng nhập tạo một "family" mới.
 *   <li>Mỗi lần refresh: token cũ bị thu hồi ({@code ROTATED}), token mới cùng family được cấp.
 *   <li>Token đã xoay vòng bị dùng lại sau thời gian ân hạn → thu hồi cả family (nghi bị đánh cắp).
 * </ul>
 */
public interface RefreshTokenService {

    /** Tạo phiên mới, trả về token gốc. */
    String startSession(Long userId, ClientInfo client);

    /** @throws com.moviebooking.exception.BusinessException {@code INVALID_REFRESH_TOKEN} */
    Rotation rotate(String rawToken, ClientInfo client);

    void revokeSession(String rawToken);

    void revokeAllForUser(Long userId, RevokeReason reason);

    record Rotation(Long userId, String newRawToken) {}
}
