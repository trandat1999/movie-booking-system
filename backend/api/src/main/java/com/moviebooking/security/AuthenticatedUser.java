package com.moviebooking.security;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Người dùng đã xác thực, dựng lại từ access token (không truy vấn CSDL ở mỗi request).
 *
 * <p>Authority gồm {@code ROLE_<code>} cho từng vai trò và mã quyền dạng {@code resource:action}.
 */
public record AuthenticatedUser(Long id, String email, String fullName, Set<String> roles, Set<String> permissions)
        implements Principal {

    /** Tên principal = id người dùng; dùng làm đích {@code /user/{id}/queue/...} của WebSocket. */
    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public Collection<GrantedAuthority> authorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(roles.size() + permissions.size());
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        permissions.forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
        return authorities;
    }
}
