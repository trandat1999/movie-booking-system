package com.moviebooking.modules.user.repository;

import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.enums.UserStatus;
import jakarta.persistence.criteria.Join;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/** Điều kiện lọc danh sách người dùng cho trang quản trị. */
public final class UserSpecifications {

    private UserSpecifications() {}

    /** Tìm gần đúng theo email, họ tên hoặc số điện thoại. */
    public static Specification<User> keyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("email")), pattern),
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(root.get("phone"), pattern));
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<User> hasRole(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<User, Role> roles = root.join("roles");
            return cb.equal(roles.get("code"), roleCode);
        };
    }
}
