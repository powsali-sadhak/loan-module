package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.LoanActionDTO;
import com.powsali.loan_module.dto.LoanRequestDTO;
import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.security.CustomUserDetails;
import com.powsali.loan_module.service.LoanActionService;
import com.powsali.loan_module.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanActionService loanActionService;

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'DDO')")
    public ResponseEntity<LoanApplication> applyLoan(@RequestBody LoanRequestDTO request,
                                                     @AuthenticationPrincipal CustomUserDetails principal) {
        LoanApplication loan = loanService.applyLoan(
                request.getApplicantId(),
                principal.getUser().getId(),
                request.getAmount(),
                request.getPurpose()
        );
        return ResponseEntity.ok(loan);
    }

    @PostMapping("/action")
    public ResponseEntity<LoanAction> takeAction(@RequestBody LoanActionDTO actionDTO,
                                                 @AuthenticationPrincipal CustomUserDetails principal) {
        LoanAction action = loanActionService.recordAction(
                actionDTO.getLoanId(),
                principal.getUser().getId(),
                actionDTO.getActionType(),
                actionDTO.getRemarks()
        );
        return ResponseEntity.ok(action);
    }

    @GetMapping("/{loanId}")
    public ResponseEntity<LoanApplication> getLoan(@PathVariable Long loanId,
                                                    @AuthenticationPrincipal CustomUserDetails principal) {
        return loanService.getLoanById(loanId)
                .filter(loan -> principal.getUser().getRole() == Role.ADMIN
                        || loan.getApplicant().getId().equals(principal.getUser().getId())
                        || loan.getAppliedBy().getId().equals(principal.getUser().getId())
                        || loan.getCurrentApproverRole() == principal.getUser().getRole())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanApplication>> getLoansByUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        if (principal.getUser().getRole() != Role.ADMIN
                && !principal.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(loanService.getLoansByApplicant(userId));
    }

    @GetMapping("/{loanId}/actions")
    public ResponseEntity<List<LoanAction>> getLoanActions(@PathVariable Long loanId,
                                                           @AuthenticationPrincipal CustomUserDetails principal) {
        boolean canView = loanService.getLoanById(loanId)
                .map(loan -> principal.getUser().getRole() == Role.ADMIN
                        || loan.getApplicant().getId().equals(principal.getUser().getId())
                        || loan.getAppliedBy().getId().equals(principal.getUser().getId())
                        || loan.getCurrentApproverRole() == principal.getUser().getRole())
                .orElse(false);
        if (!canView) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(loanActionService.getActionsForLoan(loanId));
    }
}
