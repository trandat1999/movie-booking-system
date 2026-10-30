package com.moviebooking.modules.user.service.impl;

import com.moviebooking.common.util.PhoneNumbers;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.user.dto.request.ChangePasswordRequest;
import com.moviebooking.modules.user.dto.request.CreateUserCommand;
import com.moviebooking.modules.user.dto.request.UpdateProfileRequest;
import com.moviebooking.modules.user.dto.response.UserProfileResponse;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.event.UserSessionsInvalidatedEvent;
import com.moviebooking.modules.user.mapper.UserMapper;
import com.moviebooking.modules.user.repository.RoleRepository;
import com.moviebooking.modules.user.repository.UserRepository;
import com.moviebooking.modules.user.service.UserService;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Long, com.moviebooking.modules.user.dto.response.UserBrief> briefs(java.util.Collection<Long> ids) {
        java.util.Map<Long, com.moviebooking.modules.user.dto.response.UserBrief> result = new java.util.HashMap<>();
        userRepository.findAllById(ids).forEach(u -> result.put(
                u.getId(), new com.moviebooking.modules.user.dto.response.UserBrief(u.getId(), u.getFullName(), u.getEmail(), u.getPhone())));
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findIdByEmail(String email) {
        return userRepository.findWithRolesByEmail(email.trim().toLowerCase(java.util.Locale.ROOT)).map(User::getId);
    }

    @Override
    @Transactional
    public User createCustomer(CreateUserCommand command) {
        String email = normalizeEmail(command.email());
        String phone = PhoneNumbers.normalize(command.phone());
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        Role customer = roleRepository
                .findByCode(Role.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER is missing"));

        User user = new User();
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(command.rawPassword()));
        user.setFullName(command.fullName().trim());
        user.setDateOfBirth(command.dateOfBirth());
        user.getRoles().add(customer);
        return userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findForLogin(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }
        String value = identifier.trim();
        return value.contains("@")
                ? userRepository.findWithRolesByEmail(normalizeEmail(value))
                : userRepository.findWithRolesByPhone(PhoneNumbers.normalize(value));
    }

    @Override
    @Transactional(readOnly = true)
    public User getWithRoles(Long userId) {
        return userRepository
                .findWithRolesById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public void recordSuccessfulLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> user.setLastLoginAt(clock.instant()));
    }

    @Override
    public UserProfileResponse toProfile(User user) {
        return userMapper.toProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        return userMapper.toProfile(getWithRoles(userId));
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = getWithRoles(userId);
        String phone = PhoneNumbers.normalize(request.phone());
        if (phone != null && userRepository.existsByPhoneAndIdNot(phone, userId)) {
            throw new BusinessException(ErrorCode.PHONE_ALREADY_EXISTS);
        }
        user.setFullName(request.fullName().trim());
        user.setPhone(phone);
        user.setDateOfBirth(request.dateOfBirth());
        user.setAvatarUrl(blankToNull(request.avatarUrl()));
        return userMapper.toProfile(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = getWithRoles(userId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CURRENT_PASSWORD);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        eventPublisher.publishEvent(new UserSessionsInvalidatedEvent(userId));
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
