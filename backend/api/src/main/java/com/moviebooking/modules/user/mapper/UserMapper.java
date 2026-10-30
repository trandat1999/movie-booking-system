package com.moviebooking.modules.user.mapper;

import com.moviebooking.modules.user.dto.response.AdminUserResponse;
import com.moviebooking.modules.user.dto.response.PermissionResponse;
import com.moviebooking.modules.user.dto.response.RoleResponse;
import com.moviebooking.modules.user.dto.response.RoleSummaryResponse;
import com.moviebooking.modules.user.dto.response.UserProfileResponse;
import com.moviebooking.modules.user.entity.Permission;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "roles", expression = "java(java.util.List.copyOf(user.roleCodes()))")
    @Mapping(target = "permissions", expression = "java(java.util.List.copyOf(user.permissionCodes()))")
    UserProfileResponse toProfile(User user);

    AdminUserResponse toAdminUser(User user);

    RoleSummaryResponse toRoleSummary(Role role);

    RoleResponse toRole(Role role);

    PermissionResponse toPermission(Permission permission);

    default List<RoleSummaryResponse> toRoleSummaries(Set<Role> roles) {
        return roles.stream()
                .sorted(Comparator.comparing(Role::getId))
                .map(this::toRoleSummary)
                .toList();
    }

    default List<PermissionResponse> toPermissions(Set<Permission> permissions) {
        return permissions.stream()
                .sorted(Comparator.comparing(Permission::getId))
                .map(this::toPermission)
                .toList();
    }
}
