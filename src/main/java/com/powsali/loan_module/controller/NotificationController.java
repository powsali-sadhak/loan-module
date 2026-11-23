package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.NotificationResponseDTO;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.repository.UserRepository;
import com.powsali.loan_module.security.CustomUserDetails;
import com.powsali.loan_module.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;
    private final UserRepository users;

    private User resolveUser(CustomUserDetails principal, Long demoUserId) {
        if (principal != null && principal.getUser() != null) {
            return principal.getUser();
        }
        if (demoUserId == null) {
            throw new IllegalArgumentException(
                    "User context is missing. Select a demo user or sign in again.");
        }
        return users.findById(demoUserId)
                .orElseThrow(() -> new IllegalArgumentException("Demo user not found: " + demoUserId));
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> list(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {
        return ResponseEntity.ok(service.list(resolveUser(principal, demoUserId).getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {
        long count = service.unreadCount(resolveUser(principal, demoUserId).getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {
        return ResponseEntity.ok(service.markRead(
                notificationId, resolveUser(principal, demoUserId).getId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Long>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {
        long updated = service.markAllRead(resolveUser(principal, demoUserId).getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
