package com.moviebooking.modules.auth.event;

import com.moviebooking.modules.auth.enums.RevokeReason;
import com.moviebooking.modules.auth.service.RefreshTokenService;
import com.moviebooking.modules.user.event.UserSessionsInvalidatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Thu hồi mọi refresh token khi người dùng đổi mật khẩu hoặc bị khóa (cùng transaction với thao tác đó). */
@Component
@RequiredArgsConstructor
public class UserSessionsInvalidatedListener {

    private final RefreshTokenService refreshTokenService;

    @EventListener
    public void on(UserSessionsInvalidatedEvent event) {
        refreshTokenService.revokeAllForUser(event.userId(), RevokeReason.SESSIONS_INVALIDATED);
    }
}
