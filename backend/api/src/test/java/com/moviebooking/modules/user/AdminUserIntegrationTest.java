package com.moviebooking.modules.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.moviebooking.modules.audit.repository.AuditLogRepository;
import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.support.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class AdminUserIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AuditLogRepository auditLogRepository;

    private User admin;
    private Session adminSession;

    @BeforeEach
    void setUp() throws Exception {
        admin = createUser(uniqueEmail("admin"), Role.ADMIN);
        adminSession = login(admin.getEmail(), PASSWORD);
    }

    @Test
    void adminApiEnforcesAuthenticationAndPermissions() throws Exception {
        Session customer = login(createUser(uniqueEmail("cus"), Role.CUSTOMER).getEmail(), PASSWORD);
        Session staff = login(createUser(uniqueEmail("staff"), Role.STAFF).getEmail(), PASSWORD);

        mockMvc.perform(get("/api/v1/admin/users")).andExpect(status().isUnauthorized());
        // Khách hàng không có admin:access → bị chặn ở tầng URL
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", customer.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        // Nhân viên có admin:access nhưng không có user:read → bị chặn bởi @PreAuthorize
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", staff.bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/admin/users").header("Authorization", adminSession.bearer()))
                .andExpect(status().isOk());
    }

    @Test
    void searchFiltersByKeywordAndRole() throws Exception {
        String marker = "kw" + System.nanoTime();
        createUser(marker + "-a@test.local", Role.CUSTOMER);
        createUser(marker + "-b@test.local", Role.STAFF);

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("q", marker)
                        .header("Authorization", adminSession.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
        mockMvc.perform(get("/api/v1/admin/users")
                        .param("q", marker)
                        .param("role", Role.STAFF)
                        .header("Authorization", adminSession.bearer()))
                .andExpect(jsonPath("$.data.items", hasSize(1)))
                .andExpect(jsonPath("$.data.items[0].roles[0].code").value(Role.STAFF));
    }

    @Test
    void lockingUserRevokesSessionsAndIsAudited() throws Exception {
        User target = createUser(uniqueEmail("target"), Role.CUSTOMER);
        Session targetSession = login(target.getEmail(), PASSWORD);

        updateStatus(adminSession, target.getId(), "LOCKED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LOCKED"));

        mockMvc.perform(post("/api/v1/auth/refresh").cookie(targetSession.refreshCookie()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("identifier", target.getEmail(), "password", PASSWORD))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_LOCKED"));

        assertThat(auditLogRepository.findAll())
                .anySatisfy(log -> {
                    assertThat(log.getAction()).isEqualTo("USER_STATUS_UPDATE");
                    assertThat(log.getResourceId()).isEqualTo(String.valueOf(target.getId()));
                    assertThat(log.getActorId()).isEqualTo(admin.getId());
                    assertThat(log.getData()).isEqualTo(Map.of("status", "LOCKED"));
                });

        updateStatus(adminSession, target.getId(), "ACTIVE").andExpect(status().isOk());
        assertThat(login(target.getEmail(), PASSWORD).accessToken()).isNotBlank();
    }

    @Test
    void adminCannotModifyOwnAccount() throws Exception {
        updateStatus(adminSession, admin.getId(), "LOCKED")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_MODIFY_OWN_ACCOUNT"));
        updateRoles(adminSession, admin.getId(), List.of(Role.CUSTOMER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("CANNOT_MODIFY_OWN_ACCOUNT"));
    }

    @Test
    void roleChangeAppliesAfterRefresh() throws Exception {
        User target = createUser(uniqueEmail("promote"), Role.CUSTOMER);
        Session before = login(target.getEmail(), PASSWORD);

        updateRoles(adminSession, target.getId(), List.of(Role.STAFF, Role.CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasSize(2)));
        updateRoles(adminSession, target.getId(), List.of("NO_SUCH_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_FOUND"));

        Session after = Session.from(mockMvc.perform(post("/api/v1/auth/refresh").cookie(before.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.permissions", hasItem("ticket:checkin")))
                .andReturn());
        mockMvc.perform(get("/api/v1/admin/audit-logs").header("Authorization", after.bearer()))
                .andExpect(status().isForbidden());
    }

    @Test
    void rolesAndAuditLogsAreListed() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles").header("Authorization", adminSession.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value(Role.ADMIN))
                .andExpect(jsonPath("$.data[0].permissions[*].code", hasItem("user:write")));
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .param("action", "LOGIN")
                        .header("Authorization", adminSession.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[*].action", hasItem("LOGIN")));
    }

    private ResultActions updateStatus(Session session, Long userId, String status) throws Exception {
        return mockMvc.perform(patch("/api/v1/admin/users/{id}/status", userId)
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", status))));
    }

    private ResultActions updateRoles(Session session, Long userId, List<String> roles) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/users/{id}/roles", userId)
                .header("Authorization", session.bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("roleCodes", roles))));
    }
}
