package com.moviebooking.modules.user.seed;

import com.moviebooking.modules.user.entity.Role;
import com.moviebooking.modules.user.entity.User;
import com.moviebooking.modules.user.repository.RoleRepository;
import com.moviebooking.modules.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Tạo tài khoản mẫu cho môi trường dev (chỉ chạy với profile {@code dev}, bỏ qua tài khoản đã tồn tại).
 *
 * <p>Tài khoản và mật khẩu dev được liệt kê trong README.md.
 */
@Slf4j
@Profile("dev")
@Component
@RequiredArgsConstructor
public class DevUserSeeder implements ApplicationRunner {

    static final String DEV_PASSWORD = "Password123";

    private static final List<SeedUser> USERS = List.of(
            new SeedUser("admin@moviebooking.local", "0900000001", "Quản trị viên", Role.ADMIN),
            new SeedUser("staff@moviebooking.local", "0900000002", "Nhân viên rạp", Role.STAFF),
            new SeedUser("customer@moviebooking.local", "0900000003", "Khách hàng mẫu", Role.CUSTOMER));

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        for (SeedUser seed : USERS) {
            if (userRepository.existsByEmail(seed.email())) {
                continue;
            }
            Role role = roleRepository
                    .findByCode(seed.roleCode())
                    .orElseThrow(() -> new IllegalStateException("Missing role " + seed.roleCode()));
            User user = new User();
            user.setEmail(seed.email());
            user.setPhone(seed.phone());
            user.setFullName(seed.fullName());
            user.setPasswordHash(passwordEncoder.encode(DEV_PASSWORD));
            user.getRoles().add(role);
            userRepository.save(user);
            log.info("Seeded dev user {} ({})", seed.email(), seed.roleCode());
        }
    }

    private record SeedUser(String email, String phone, String fullName, String roleCode) {}
}
