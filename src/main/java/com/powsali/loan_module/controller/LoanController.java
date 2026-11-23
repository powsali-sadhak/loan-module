package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.*;
import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.LoanRepayment;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.security.CustomUserDetails;
import com.powsali.loan_module.service.LoanActionService;
import com.powsali.loan_module.service.LoanEligibilityService;
import com.powsali.loan_module.service.LoanRepaymentService;
import com.powsali.loan_module.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class LoanController {

    private static final Set<Role> PRIVILEGED_READ_ROLES =
            Set.of(Role.DDO, Role.HOO, Role.ADMIN);

    private final LoanService loanService;
    private final LoanActionService loanActionService;
    private final LoanEligibilityService eligibilityService;
    private final LoanRepaymentService repaymentService;

    @GetMapping("/eligibility/{applicantId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getEligibility(
            @PathVariable Long applicantId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        User viewer = principal.getUser();
        requireSelfOrPrivileged(viewer, applicantId);
        return ResponseEntity.ok(eligibilityService.getEligibility(applicantId));
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO')")
    public ResponseEntity<?> applyLoan(
            @RequestBody LoanRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        // The actor always comes from the authenticated identity, never the request body.
        LoanApplication loan = loanService.applyLoan(
                request.getApplicantId(),
                principal.getUser().getId(),
                request.getAmount(),
                request.getPurpose());
        return ResponseEntity.ok(toLoanResponse(loan));
    }

    @PostMapping("/action")
    @PreAuthorize("hasAnyRole('DDO','HOO')")
    public ResponseEntity<?> takeAction(
            @RequestBody LoanActionDTO actionDTO,
            @AuthenticationPrincipal CustomUserDetails principal) {
        LoanAction action = loanActionService.recordAction(
                actionDTO.getLoanId(),
                principal.getUser().getId(),
                actionDTO.getActionType(),
                actionDTO.getRemarks());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Loan action recorded successfully");
        response.put("actionId", action.getId());
        response.put("loanId", action.getLoanApplication().getId());
        response.put("actionType", action.getActionType());
        response.put("remarks", action.getRemarks());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{loanId}/resubmit")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO')")
    public ResponseEntity<?> resubmitLoan(
            @PathVariable Long loanId,
            @RequestBody LoanResubmissionDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        LoanApplication loan = loanActionService.resubmitLoan(
                loanId,
                principal.getUser().getId(),
                request.getAmount(),
                request.getPurpose(),
                request.getRemarks());
        return ResponseEntity.ok(toLoanResponse(loan));
    }

    @PostMapping("/{loanId}/repayments")
    @PreAuthorize("hasAnyRole('DDO','HOO','ADMIN')")
    public ResponseEntity<?> recordRepayment(
            @PathVariable Long loanId,
            @RequestBody LoanRepaymentRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        LoanRepayment repayment = repaymentService.recordRepayment(
                loanId,
                principal.getUser().getId(),
                request.getAmount(),
                request.getRemarks());
        return ResponseEntity.ok(toRepaymentResponse(repayment));
    }

    @GetMapping("/{loanId}/repayments")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getRepayments(
            @PathVariable Long loanId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        LoanApplication loan = findLoan(loanId);
        requireLoanAccess(loan, principal.getUser());
        List<Map<String, Object>> repayments = repaymentService.getRepayments(loanId)
                .stream()
                .map(this::toRepaymentResponse)
                .toList();
        return ResponseEntity.ok(repayments);
    }

    @PostMapping("/{loanId}/recovery-schedule")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO')")
    public ResponseEntity<?> createRecoverySchedule(
            @PathVariable Long loanId,
            @RequestBody LoanRecoveryScheduleRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(repaymentService.createRecoverySchedule(
                loanId, principal.getUser().getId(), request.getMonthlyAmount()));
    }

    @GetMapping("/recovery-schedules")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getRecoverySchedule(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(repaymentService.getRecoverySchedule(
                principal.getUser().getId()));
    }

    @GetMapping("/recoveries")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getRecoveryHistory(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(repaymentService.getRecoveryHistory(
                principal.getUser().getId()));
    }

    @GetMapping("/{loanId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getLoan(
            @PathVariable Long loanId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        LoanApplication loan = findLoan(loanId);
        requireLoanAccess(loan, principal.getUser());
        return ResponseEntity.ok(toLoanResponse(loan));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getLoansByApplicant(
            @PathVariable Long userId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        requireSelfOrPrivileged(principal.getUser(), userId);
        List<Map<String, Object>> loans = loanService.getLoansByApplicant(userId)
                .stream()
                .map(this::toLoanResponse)
                .toList();
        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{loanId}/actions")
    @PreAuthorize("hasAnyRole('EMPLOYEE','DDO','HOO','ADMIN')")
    public ResponseEntity<?> getLoanActions(
            @PathVariable Long loanId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        LoanApplication loan = findLoan(loanId);
        requireLoanAccess(loan, principal.getUser());
        List<Map<String, Object>> actions = loanActionService.getActionsForLoan(loanId)
                .stream()
                .map(this::toActionResponse)
                .toList();
        return ResponseEntity.ok(actions);
    }

    private LoanApplication findLoan(Long loanId) {
        return loanService.getLoanById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));
    }

    private void requireSelfOrPrivileged(User viewer, Long targetUserId) {
        if (viewer.getId().equals(targetUserId)
                || PRIVILEGED_READ_ROLES.contains(viewer.getRole())) {
            return;
        }
        throw new AccessDeniedException("You can only view your own loan information.");
    }

    private void requireLoanAccess(LoanApplication loan, User viewer) {
        if (PRIVILEGED_READ_ROLES.contains(viewer.getRole())
                || loan.getApplicant().getId().equals(viewer.getId())
                || (loan.getAppliedBy() != null
                && loan.getAppliedBy().getId().equals(viewer.getId()))) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to view this loan application.");
    }

    private Map<String, Object> toLoanResponse(LoanApplication loan) {
        Map<String, Object> response = new HashMap<>();
        response.put("loanId", loan.getId());
        response.put("applicantId", loan.getApplicant().getId());
        response.put("applicantName", loan.getApplicant().getName());
        response.put("applicantRole", loan.getApplicant().getRole());
        response.put("applicantGroup", loan.getApplicant().getServiceGroup());
        response.put("appliedById", loan.getAppliedBy() != null ? loan.getAppliedBy().getId() : null);
        response.put("amount", loan.getAmount());
        response.put("purpose", loan.getPurpose());
        response.put("status", loan.getStatus());
        response.put("currentOwner", loan.getCurrentOwner());
        response.put("currentStepOrder", loan.getCurrentStepOrder());
        response.put("currentApproverRole", loan.getCurrentApproverRole());
        response.put("createdAt", loan.getCreatedAt());
        response.put("updatedAt", loan.getUpdatedAt());
        return response;
    }

    private Map<String, Object> toActionResponse(LoanAction action) {
        Map<String, Object> response = new HashMap<>();
        response.put("actionId", action.getId());
        response.put("actionType", action.getActionType());
        response.put("remarks", action.getRemarks());
        response.put("actionTime", action.getActionTime());
        response.put("actionBy", action.getActionBy() != null ? action.getActionBy().getName() : null);
        return response;
    }

    private Map<String, Object> toRepaymentResponse(LoanRepayment repayment) {
        Map<String, Object> response = new HashMap<>();
        response.put("repaymentId", repayment.getId());
        response.put("loanId", repayment.getLoanApplication().getId());
        response.put("amount", repayment.getAmount());
        response.put("remarks", repayment.getRemarks());
        response.put("receivedAt", repayment.getReceivedAt());
        response.put("recordedBy", repayment.getRecordedBy() != null
                ? repayment.getRecordedBy().getName() : null);
        return response;
    }
}
