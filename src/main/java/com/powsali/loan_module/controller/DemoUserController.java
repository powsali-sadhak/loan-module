package com.powsali.loan_module.controller;

import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/demo/users")
@Profile("!prod")
@RequiredArgsConstructor
public class DemoUserController {

    private final UserRepository userRepository;

    @GetMapping
    public List<DemoUserResponse> getDemoUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getUsername() != null
                        && user.getUsername().matches("(employee|ddo|hoo|support)\\d+"))
                .sorted(Comparator
                        .comparingInt((User user) -> roleOrder(user.getUsername()))
                        .thenComparingInt(user -> accountNumber(user.getUsername())))
                .map(user -> new DemoUserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getName(),
                        user.getRole().name(),
                        user.getServiceGroup(),
                        user.getEmail()))
                .toList();
    }

    private int roleOrder(String username) {
        if (username.startsWith("employee")) return 1;
        if (username.startsWith("ddo")) return 2;
        if (username.startsWith("hoo")) return 3;
        return 4;
    }

    private int accountNumber(String username) {
        return Integer.parseInt(username.replaceAll("\\D", ""));
    }

    public record DemoUserResponse(
            Long id,
            String username,
            String name,
            String role,
            String serviceGroup,
            String email) {
    }
}
