package com.moviebooking.modules.user.service;

import com.moviebooking.modules.user.dto.request.ChangePasswordRequest;
import com.moviebooking.modules.user.dto.request.CreateUserCommand;
import com.moviebooking.modules.user.dto.request.UpdateProfileRequest;
import com.moviebooking.modules.user.dto.response.UserProfileResponse;
import com.moviebooking.modules.user.entity.User;
import java.util.Optional;

/** Nghiệp vụ tài khoản của chính người dùng; cũng là cổng duy nhất để module khác đọc/tạo người dùng. */
public interface UserService {

    /** Tạo tài khoản khách hàng (vai trò CUSTOMER). */
    User createCustomer(CreateUserCommand command);

    /** Tìm người dùng theo email hoặc số điện thoại (kèm vai trò, quyền). */
    Optional<User> findForLogin(String identifier);

    /** Lấy người dùng kèm vai trò, quyền; lỗi nếu không tồn tại. */
    User getWithRoles(Long userId);

    void recordSuccessfulLogin(Long userId);

    UserProfileResponse toProfile(User user);

    UserProfileResponse getProfile(Long userId);

    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);

    /** Đổi mật khẩu và thu hồi mọi phiên đăng nhập. */
    void changePassword(Long userId, ChangePasswordRequest request);

    /** Thông tin ngắn của nhiều người dùng (màn quản trị đơn/nhóm). */
    java.util.Map<Long, com.moviebooking.modules.user.dto.response.UserBrief> briefs(java.util.Collection<Long> ids);

    Optional<Long> findIdByEmail(String email);
}
