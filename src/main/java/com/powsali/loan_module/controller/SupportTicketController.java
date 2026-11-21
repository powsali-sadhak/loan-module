package com.powsali.loan_module.controller;
import com.powsali.loan_module.dto.*;import com.powsali.loan_module.security.CustomUserDetails;import com.powsali.loan_module.service.SupportTicketService;
import lombok.RequiredArgsConstructor;import org.springframework.http.HttpStatus;import org.springframework.http.ResponseEntity;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/support/tickets") @RequiredArgsConstructor
public class SupportTicketController {
 private final SupportTicketService service;
 @PostMapping @PreAuthorize("isAuthenticated()") public ResponseEntity<TicketResponseDTO> create(@RequestBody TicketCreateRequestDTO req,@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.status(HttpStatus.CREATED).body(service.create(p.getUser(),req));}
 @GetMapping("/my") public ResponseEntity<List<TicketResponseDTO>> mine(@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.ok(service.mine(p.getUser()));}
 @GetMapping("/queue") @PreAuthorize("hasAnyRole('SUPPORT','ADMIN')") public ResponseEntity<List<TicketResponseDTO>> queue(@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.ok(service.queue(p.getUser()));}
 @GetMapping("/{ticketId}") public ResponseEntity<TicketResponseDTO> get(@PathVariable Long ticketId,@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.ok(service.get(ticketId,p.getUser()));}
 @PostMapping("/{ticketId}/comments") public ResponseEntity<TicketResponseDTO> comment(@PathVariable Long ticketId,@RequestBody TicketCommentRequestDTO req,@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.ok(service.comment(ticketId,req,p.getUser()));}
 @PatchMapping("/{ticketId}/assign") @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<TicketResponseDTO> assign(@PathVariable Long ticketId,@RequestBody TicketAssignmentRequestDTO req,@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.ok(service.assign(ticketId,req.getAssigneeId(),p.getUser()));}
 @PatchMapping("/{ticketId}/status") public ResponseEntity<TicketResponseDTO> status(@PathVariable Long ticketId,@RequestBody TicketStatusRequestDTO req,@AuthenticationPrincipal CustomUserDetails p){return ResponseEntity.ok(service.status(ticketId,req.getStatus(),req.getResolution(),p.getUser()));}
}
