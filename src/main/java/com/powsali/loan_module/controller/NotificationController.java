package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.NotificationResponseDTO;
import com.powsali.loan_module.security.CustomUserDetails;
import com.powsali.loan_module.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public ResponseEntity<List<NotificationResponseDTO>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.list(principal.getUser().getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails principal) {
        long count = service.unreadCount(principal.getUser().getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDTO> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.markRead(
                notificationId, principal.getUser().getId()));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Long>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails principal) {
        long updated = service.markAllRead(principal.getUser().getId());
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
