package com.moviebooking.modules.user.service;

import com.moviebooking.common.response.PageResponse;
import com.moviebooking.modules.user.dto.response.AdminUserResponse;
import com.moviebooking.modules.user.dto.response.RoleResponse;
import com.moviebooking.modules.user.enums.UserStatus;
import java.util.List;
import java.util.Set;

/** Quản lý người dùng & vai trò cho trang quản trị (USRADM). */
public interface AdminUserService {

    PageResponse<AdminUserResponse> search(String keyword, UserStatus status, String roleCode, int page, int size);

    AdminUserResponse get(Long userId);

    /** Khóa/mở khóa; khóa thì thu hồi mọi phiên đăng nhập. Không được tự khóa chính mình. */
    AdminUserResponse updateStatus(Long actorId, Long userId, UserStatus status);

    /** Thay toàn bộ vai trò của người dùng. Không được tự đổi vai trò của chính mình. */
    AdminUserResponse updateRoles(Long actorId, Long userId, Set<String> roleCodes);

    List<RoleResponse> listRoles();
}
