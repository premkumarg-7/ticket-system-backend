package e2d.auth.dataInitializer;

import e2d.auth.domain.Role;
import e2d.auth.domain.User;
import e2d.auth.repository.RoleRepository;
import e2d.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Order(1)
public class DataBootstrapRunner implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.username:admin}")
    private String adminUsername;

    @Value("${app.bootstrap.admin.email:admin@test.com}")
    private String adminemail;

    @Value("${app.bootstrap.admin.password:admin123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        // 1️⃣ Ensure roles exist
        Role roleUser = createRoleIfMissing("ROLE_USER");
        Role roleAdmin = createRoleIfMissing("ROLE_ADMIN");
        createRoleIfMissing("ROLE_AGENT");

        // 2️⃣ Ensure admin exists
        userRepository.findByUsername(adminUsername)
                .orElseGet(() -> {
                    User admin = new User();
                    admin.setUsername(adminUsername);
                    admin.setEmail(adminemail);
                    admin.setPassword(passwordEncoder.encode(adminPassword));
                    admin.setRoles(Set.of(roleAdmin));
                    return userRepository.save(admin);
                });
    }

    private Role createRoleIfMissing(String roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() ->
                        roleRepository.save(new Role(null, roleName))
                );
    }
}

