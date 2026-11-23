package com.powsali.loan_module.controller;

import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

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
                .filter(user -> {
                    String username = user.getUsername();
                    return username != null
                            && (username.matches("(employee|ddo|hoo|support)\\d+")
                            || "admin".equals(username));
                })
                .sorted(Comparator
                        .comparingInt((User user) -> roleOrder(user.getUsername()))
                        .thenComparingInt(user -> accountNumber(user.getUsername())))
                .map(this::toResponse)
                .toList();
    }

    /**
     * Local-demo-only settings endpoint. A selected demo identity can only
     * update its own WhatsApp destination and opt-in preference.
     */
    @PatchMapping("/{userId}/whatsapp-settings")
    public DemoUserResponse updateWhatsAppSettings(
            @PathVariable Long userId,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId,
            @RequestBody WhatsAppSettingsRequest request) {

        if (demoUserId == null || !demoUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only update WhatsApp settings for the selected demo user.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Demo user not found."));

        String mobileNumber = user.getMobileNumber();
        if (request.mobileNumber() != null && !request.mobileNumber().isBlank()) {
            mobileNumber = normalizeMobileNumber(request.mobileNumber());
        }

        if (request.whatsappNotificationsEnabled() && mobileNumber == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Enter a valid mobile number before enabling WhatsApp notifications.");
        }

        user.setMobileNumber(mobileNumber);
        user.setWhatsappNotificationsEnabled(request.whatsappNotificationsEnabled());
        return toResponse(userRepository.save(user));
    }

    private String normalizeMobileNumber(String input) {
        String compact = input.trim().replaceAll("[\\s()-]", "");
        if (compact.matches("\\+[1-9][0-9]{7,14}")) {
            return compact;
        }
        String digits = compact.replaceAll("\\D", "");
        if (digits.matches("[0-9]{10}")) {
            return "+91" + digits;
        }
        if (digits.matches("91[0-9]{10}")) {
            return "+" + digits;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Enter a valid number in international format, such as +919876543210.");
    }

    private DemoUserResponse toResponse(User user) {
        return new DemoUserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getRole().name(),
                user.getServiceGroup(),
                user.getEmail(),
                user.getMobileNumber(),
                user.isWhatsappNotificationsEnabled());
    }

    private int roleOrder(String username) {
        if (username.startsWith("employee")) return 1;
        if (username.startsWith("ddo")) return 2;
        if (username.startsWith("hoo")) return 3;
        if (username.startsWith("support")) return 4;
        return 5;
    }

    private int accountNumber(String username) {
        String digits = username.replaceAll("\\D", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    public record WhatsAppSettingsRequest(
            String mobileNumber,
            boolean whatsappNotificationsEnabled) {
    }

    public record DemoUserResponse(
            Long id,
            String username,
            String name,
            String role,
            String serviceGroup,
            String email,
            String mobileNumber,
            boolean whatsappNotificationsEnabled) {
    }
}
