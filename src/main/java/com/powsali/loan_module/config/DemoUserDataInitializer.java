package com.powsali.loan_module.config;

import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Creates an idempotent set of local-only demo accounts.
 * Demo passwords are configured locally or generated randomly at startup.
 */
@Component
@Profile("local-demo & !prod")
@RequiredArgsConstructor
public class DemoUserDataInitializer implements CommandLineRunner {

    @Value("${app.demo.password:}")
    private String configuredDemoPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // A shared demo password is never committed. Configure DEMO_PASSWORD
        // locally when interactive demo login is needed; otherwise use a
        // random value because the local demo identity switcher does not need it.
        String demoPassword = configuredDemoPassword == null || configuredDemoPassword.isBlank()
                ? UUID.randomUUID().toString()
                : configuredDemoPassword;
        String passwordHash = passwordEncoder.encode(demoPassword);

        for (DemoAccount account : accounts()) {
            User user = userRepository.findByUsername(account.username())
                    .orElseGet(User::new);

            user.setUsername(account.username());
            user.setName(account.name());
            user.setEmail(account.email());
            user.setRole(account.role());
            user.setServiceGroup(account.serviceGroup());
            user.setPassword(passwordHash);

            userRepository.save(user);
        }
    }

    private List<DemoAccount> accounts() {
        return List.of(
            new DemoAccount("admin", "Sample Administrator", "admin@loanmodule.com", Role.ADMIN, null),

            new DemoAccount("employee1", "Sample Employee 01", "employee1@test.com", Role.EMPLOYEE, "A"),
            new DemoAccount("employee2", "Sample Employee 02", "employee2@test.com", Role.EMPLOYEE, "B"),
            new DemoAccount("employee3", "Sample Employee 03", "employee3@test.com", Role.EMPLOYEE, "C"),
            new DemoAccount("employee4", "Sample Employee 04", "employee4@test.com", Role.EMPLOYEE, "D"),
            new DemoAccount("employee5", "Sample Employee 05", "employee5@test.com", Role.EMPLOYEE, "A"),
            new DemoAccount("employee6", "Sample Employee 06", "employee6@test.com", Role.EMPLOYEE, "B"),
            new DemoAccount("employee7", "Sample Employee 07", "employee7@test.com", Role.EMPLOYEE, "C"),
            new DemoAccount("employee8", "Sample Employee 08", "employee8@test.com", Role.EMPLOYEE, "D"),
            new DemoAccount("employee9", "Sample Employee 09", "employee9@test.com", Role.EMPLOYEE, "A"),
            new DemoAccount("employee10", "Sample Employee 10", "employee10@test.com", Role.EMPLOYEE, "B"),

            new DemoAccount("ddo1", "Sample DDO 01", "ddo1@test.com", Role.DDO, "B"),
            new DemoAccount("ddo2", "Sample DDO 02", "ddo2@test.com", Role.DDO, "B"),
            new DemoAccount("ddo3", "Sample DDO 03", "ddo3@test.com", Role.DDO, "B"),

            new DemoAccount("hoo1", "Sample HOO 01", "hoo1@test.com", Role.HOO, "A"),
            new DemoAccount("hoo2", "Sample HOO 02", "hoo2@test.com", Role.HOO, "A"),

            new DemoAccount("support1", "Sample Support 01", "support1@test.com", Role.SUPPORT, null),
            new DemoAccount("support2", "Sample Support 02", "support2@test.com", Role.SUPPORT, null)
        );
    }

    private record DemoAccount(
            String username,
            String name,
            String email,
            Role role,
            String serviceGroup) {
    }
}
