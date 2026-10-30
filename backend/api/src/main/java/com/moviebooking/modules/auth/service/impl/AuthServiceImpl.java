package com.moviebooking.modules.auth.service.impl;

import com.moviebooking.common.web.ClientInfo;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.audit.dto.request.AuditEntry;
import com.moviebooking.modules.audit.service.AuditService;
import com.moviebooking.modules.auth.dto.request.LoginRequest;
import com.moviebooking.modules.auth.dto.request.RegisterRequest;
import com.moviebooking.modules.auth.dto.response.AuthResponse;
import com.moviebooking.modules.auth.service.AuthResult;
import com.moviebooking.modules.auth.service.AuthService;
import com.moviebooking.modules.auth.service.RefreshTokenService;
import com.moviebooking.modules.user.dto.request.CreateUserCommand;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.service.UserService;
import com.moviebooking.security.AuthenticatedUser;
import com.moviebooking.security.JwtService;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    /** Dùng để so khớp khi không tìm thấy tài khoản, giữ thời gian phản hồi như nhau (chống dò tài khoản). */
    private final String dummyPasswordHash;

    public AuthServiceImpl(
            UserService userService,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            AuditService auditService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.dummyPasswordHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request, ClientInfo client) {
        User user = userService.createCustomer(new CreateUserCommand(
                request.email(), request.phone(), request.password(), request.fullName(), request.dateOfBirth()));
        auditService.record(new AuditEntry(
                "REGISTER", "user", String.valueOf(user.getId()), null, user.getId(), user.getEmail(),
                client.ipAddress()));
        return startSession(user, client);
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request, ClientInfo client) {
        Optional<User> found = userService.findForLogin(request.identifier());
        if (found.isEmpty()) {
            passwordEncoder.matches(request.password(), dummyPasswordHash);
            auditLoginFailure(request.identifier(), null, client);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLoginFailure(request.identifier(), user.getId(), client);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        if (!user.isActive()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        userService.recordSuccessfulLogin(user.getId());
        auditService.record(new AuditEntry(
                "LOGIN", "user", String.valueOf(user.getId()), null, user.getId(), user.getEmail(),
                client.ipAddress()));
        return startSession(user, client);
    }

    @Override
    @Transactional(noRollbackFor = BusinessException.class)
    public AuthResult refresh(String refreshToken, ClientInfo client) {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(refreshToken, client);
        User user = userService.getWithRoles(rotation.userId());
        if (!user.isActive()) {
            refreshTokenService.revokeSession(rotation.newRawToken());
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }
        return new AuthResult(issueAccessToken(user), rotation.newRawToken());
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenService.revokeSession(refreshToken);
    }

    private AuthResult startSession(User user, ClientInfo client) {
        String refreshToken = refreshTokenService.startSession(user.getId(), client);
        return new AuthResult(issueAccessToken(user), refreshToken);
    }

    private AuthResponse issueAccessToken(User user) {
        AuthenticatedUser principal = new AuthenticatedUser(
                user.getId(), user.getEmail(), user.getFullName(), user.roleCodes(), user.permissionCodes());
        JwtService.IssuedToken token = jwtService.issue(principal);
        return AuthResponse.bearer(token.value(), token.expiresAt(), userService.toProfile(user));
    }

    private void auditLoginFailure(String identifier, Long userId, ClientInfo client) {
        auditService.record(new AuditEntry(
                "LOGIN_FAILED",
                "user",
                userId == null ? null : String.valueOf(userId),
                Map.of("identifier", identifier),
                null,
                null,
                client.ipAddress()));
    }
}
