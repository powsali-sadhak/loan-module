package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.*;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.repository.UserRepository;
import com.powsali.loan_module.security.CustomUserDetails;
import com.powsali.loan_module.service.SupportTicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService service;
    private final UserRepository users;

    /**
     * In local demo mode, SecurityConfig permits requests without registering
     * the JWT filter. The frontend therefore sends X-Demo-User-Id.
     *
     * In an authenticated deployment, the authenticated principal takes
     * precedence and the demo header is ignored.
     */
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

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TicketResponseDTO> create(
            @RequestBody TicketCreateRequestDTO req,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(resolveUser(principal, demoUserId), req));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketResponseDTO>> mine(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.ok(service.mine(resolveUser(principal, demoUserId)));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('SUPPORT','ADMIN')")
    public ResponseEntity<List<TicketResponseDTO>> queue(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.ok(service.queue(resolveUser(principal, demoUserId)));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> get(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.ok(service.get(ticketId, resolveUser(principal, demoUserId)));
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketResponseDTO> comment(
            @PathVariable Long ticketId,
            @RequestBody TicketCommentRequestDTO req,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.ok(service.comment(ticketId, req, resolveUser(principal, demoUserId)));
    }

    @PatchMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponseDTO> assign(
            @PathVariable Long ticketId,
            @RequestBody TicketAssignmentRequestDTO req,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.ok(service.assign(
                ticketId, req.getAssigneeId(), resolveUser(principal, demoUserId)));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponseDTO> status(
            @PathVariable Long ticketId,
            @RequestBody TicketStatusRequestDTO req,
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestHeader(value = "X-Demo-User-Id", required = false) Long demoUserId) {

        return ResponseEntity.ok(service.status(
                ticketId, req.getStatus(), req.getResolution(), resolveUser(principal, demoUserId)));
    }
}
