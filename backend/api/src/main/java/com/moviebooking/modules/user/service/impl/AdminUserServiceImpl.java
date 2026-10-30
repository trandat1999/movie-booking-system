package com.moviebooking.modules.user.service.impl;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.exception.BusinessException;
import com.moviebooking.exception.ErrorCode;
import com.moviebooking.modules.user.dto.response.AdminUserResponse;
import com.moviebooking.modules.user.dto.response.RoleResponse;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.enums.UserStatus;
import com.moviebooking.modules.user.event.UserSessionsInvalidatedEvent;
import com.moviebooking.modules.user.mapper.UserMapper;
import com.moviebooking.modules.user.repository.RoleRepository;
import com.moviebooking.modules.user.repository.UserRepository;
import com.moviebooking.modules.user.repository.UserSpecifications;
import com.moviebooking.modules.user.service.AdminUserService;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(
            String keyword, UserStatus status, String roleCode, int page, int size) {
        Specification<User> spec = Specification.allOf(
                UserSpecifications.keyword(keyword),
                UserSpecifications.hasStatus(status),
                UserSpecifications.hasRole(roleCode));
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(userRepository.findAll(spec, pageable).map(userMapper::toAdminUser));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponse get(Long userId) {
        return userMapper.toAdminUser(findUser(userId));
    }

    @Override
    @Transactional
    public AdminUserResponse updateStatus(Long actorId, Long userId, UserStatus status) {
        ensureNotSelf(actorId, userId);
        User user = findUser(userId);
        if (user.getStatus() != status) {
            user.setStatus(status);
            if (status == UserStatus.LOCKED) {
                eventPublisher.publishEvent(new UserSessionsInvalidatedEvent(userId));
            }
        }
        return userMapper.toAdminUser(user);
    }

    @Override
    @Transactional
    public AdminUserResponse updateRoles(Long actorId, Long userId, Set<String> roleCodes) {
        ensureNotSelf(actorId, userId);
        User user = findUser(userId);
        List<Role> roles = roleRepository.findByCodeIn(roleCodes);
        if (roles.size() != roleCodes.size()) {
            throw new BusinessException(ErrorCode.ROLE_NOT_FOUND);
        }
        // Quyền mới có hiệu lực từ lần refresh token kế tiếp (tối đa bằng thời hạn access token).
        user.setRoles(new HashSet<>(roles));
        return userMapper.toAdminUser(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> listRoles() {
        return roleRepository.findAllByOrderByIdAsc().stream()
                .map(userMapper::toRole)
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository
                .findWithRolesById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private static void ensureNotSelf(Long actorId, Long userId) {
        if (Objects.equals(actorId, userId)) {
            throw new BusinessException(ErrorCode.CANNOT_MODIFY_OWN_ACCOUNT);
        }
    }
}
