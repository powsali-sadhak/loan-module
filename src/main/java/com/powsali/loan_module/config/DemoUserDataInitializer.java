package com.powsali.loan_module.config;

import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Creates an idempotent set of local-only demo accounts.
 * All demo accounts use the same documented test password.
 */
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class DemoUserDataInitializer implements CommandLineRunner {

    public static final String DEMO_PASSWORD = "Demo@12345";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String passwordHash = passwordEncoder.encode(DEMO_PASSWORD);

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
            new DemoAccount("admin", "System Administrator", "admin@loanmodule.com", Role.ADMIN, null),

            new DemoAccount("employee1", "Ananya Das", "employee1@test.com", Role.EMPLOYEE, "A"),
            new DemoAccount("employee2", "Arjun Ghosh", "employee2@test.com", Role.EMPLOYEE, "B"),
            new DemoAccount("employee3", "Ishita Roy", "employee3@test.com", Role.EMPLOYEE, "C"),
            new DemoAccount("employee4", "Sayan Banerjee", "employee4@test.com", Role.EMPLOYEE, "D"),
            new DemoAccount("employee5", "Riya Sen", "employee5@test.com", Role.EMPLOYEE, "A"),
            new DemoAccount("employee6", "Soumya Dey", "employee6@test.com", Role.EMPLOYEE, "B"),
            new DemoAccount("employee7", "Trisha Mukherjee", "employee7@test.com", Role.EMPLOYEE, "C"),
            new DemoAccount("employee8", "Aritra Bose", "employee8@test.com", Role.EMPLOYEE, "D"),
            new DemoAccount("employee9", "Moumita Paul", "employee9@test.com", Role.EMPLOYEE, "A"),
            new DemoAccount("employee10", "Ritwik Chakraborty", "employee10@test.com", Role.EMPLOYEE, "B"),

            new DemoAccount("ddo1", "Rajesh Chatterjee", "ddo1@test.com", Role.DDO, "B"),
            new DemoAccount("ddo2", "Madhumita Saha", "ddo2@test.com", Role.DDO, "B"),
            new DemoAccount("ddo3", "Suman Das", "ddo3@test.com", Role.DDO, "B"),

            new DemoAccount("hoo1", "Anirban Roy", "hoo1@test.com", Role.HOO, "A"),
            new DemoAccount("hoo2", "Sharmila Mukherjee", "hoo2@test.com", Role.HOO, "A"),

            new DemoAccount("support1", "Priya Sharma", "support1@test.com", Role.SUPPORT, null),
            new DemoAccount("support2", "Rahul Sen", "support2@test.com", Role.SUPPORT, null)
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
