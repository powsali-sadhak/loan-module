package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.*;
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
@PreAuthorize("isAuthenticated()")
public class SupportTicketController {

    private final SupportTicketService service;

    @PostMapping
    public ResponseEntity<TicketResponseDTO> create(
            @RequestBody TicketCreateRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(principal.getUser(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketResponseDTO>> mine(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.mine(principal.getUser()));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAnyRole('SUPPORT','ADMIN')")
    public ResponseEntity<List<TicketResponseDTO>> queue(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.queue(principal.getUser()));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponseDTO> get(
            @PathVariable Long ticketId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.get(ticketId, principal.getUser()));
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<TicketResponseDTO> comment(
            @PathVariable Long ticketId,
            @RequestBody TicketCommentRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.comment(ticketId, request, principal.getUser()));
    }

    @PatchMapping("/{ticketId}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TicketResponseDTO> assign(
            @PathVariable Long ticketId,
            @RequestBody TicketAssignmentRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.assign(
                ticketId, request.getAssigneeId(), principal.getUser()));
    }

    @PatchMapping("/{ticketId}/status")
    public ResponseEntity<TicketResponseDTO> status(
            @PathVariable Long ticketId,
            @RequestBody TicketStatusRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(service.status(
                ticketId, request.getStatus(), request.getResolution(), principal.getUser()));
    }
}
