package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.LoanActionDTO;
import com.powsali.loan_module.dto.LoanRepaymentRequestDTO;
import com.powsali.loan_module.dto.LoanRecoveryScheduleRequestDTO;
import com.powsali.loan_module.dto.LoanRequestDTO;
import com.powsali.loan_module.dto.LoanResubmissionDTO;
import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.LoanRepayment;
import com.powsali.loan_module.service.LoanActionService;
import com.powsali.loan_module.service.LoanEligibilityService;
import com.powsali.loan_module.service.LoanRepaymentService;
import com.powsali.loan_module.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanActionService loanActionService;
    private final LoanEligibilityService eligibilityService;
    private final LoanRepaymentService repaymentService;

    // Current group entitlement, financial-year usage and 70% recovery status.
    @GetMapping("/eligibility/{applicantId}")
    public ResponseEntity<?> getEligibility(@PathVariable Long applicantId) {
        return ResponseEntity.ok(eligibilityService.getEligibility(applicantId));
    }

    // Apply for a new loan.
    @PostMapping("/apply")
    public ResponseEntity<?> applyLoan(@RequestBody LoanRequestDTO request) {
        LoanApplication loan = loanService.applyLoan(
                request.getApplicantId(),
                request.getAppliedById(),
                request.getAmount(),
                request.getPurpose());

        return ResponseEntity.ok(toLoanResponse(loan));
    }

    // Approve, reject, forward or request revision.
    @PostMapping("/action")
    public ResponseEntity<?> takeAction(@RequestBody LoanActionDTO actionDTO) {
        LoanAction action = loanActionService.recordAction(
                actionDTO.getLoanId(),
                actionDTO.getUserId(),
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

    // Resubmit a loan after revision is requested.
    @PostMapping("/{loanId}/resubmit")
    public ResponseEntity<?> resubmitLoan(
            @PathVariable Long loanId,
            @RequestBody LoanResubmissionDTO request) {

        LoanApplication loan = loanActionService.resubmitLoan(
                loanId,
                request.getEmployeeId(),
                request.getAmount(),
                request.getPurpose(),
                request.getRemarks());

        return ResponseEntity.ok(toLoanResponse(loan));
    }

    // Record principal recovery for an approved loan.
    // Only a different DDO, HOO or ADMIN may record a repayment.
    @PostMapping("/{loanId}/repayments")
    public ResponseEntity<?> recordRepayment(
            @PathVariable Long loanId,
            @RequestBody LoanRepaymentRequestDTO request) {

        LoanRepayment repayment = repaymentService.recordRepayment(
                loanId,
                request.getRecordedById(),
                request.getAmount(),
                request.getRemarks());

        return ResponseEntity.ok(toRepaymentResponse(repayment));
    }

    @GetMapping("/{loanId}/repayments")
    public ResponseEntity<?> getRepayments(@PathVariable Long loanId) {
        List<Map<String, Object>> repayments = repaymentService.getRepayments(loanId)
                .stream()
                .map(this::toRepaymentResponse)
                .toList();

        return ResponseEntity.ok(repayments);
    }

    // Create a zero-interest monthly schedule selected by the borrower.
    @PostMapping("/{loanId}/recovery-schedule")
    public ResponseEntity<?> createRecoverySchedule(
            @PathVariable Long loanId,
            @RequestBody LoanRecoveryScheduleRequestDTO request) {
        return ResponseEntity.ok(repaymentService.createRecoverySchedule(
                loanId, request.getApplicantId(), request.getMonthlyAmount()));
    }

    // Employee-scoped or department-wide scheduled instalments, based on viewer role.
    @GetMapping("/recovery-schedules")
    public ResponseEntity<?> getRecoverySchedule(@RequestParam Long viewerId) {
        return ResponseEntity.ok(repaymentService.getRecoverySchedule(viewerId));
    }

    // Employee-scoped or department-wide recovery register, based on viewer role.
    @GetMapping("/recoveries")
    public ResponseEntity<?> getRecoveryHistory(@RequestParam Long viewerId) {
        return ResponseEntity.ok(repaymentService.getRecoveryHistory(viewerId));
    }

    // Get loan details.
    @GetMapping("/{loanId}")
    public ResponseEntity<?> getLoan(@PathVariable Long loanId) {
        LoanApplication loan = loanService.getLoanById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        return ResponseEntity.ok(toLoanResponse(loan));
    }

    // Get all loans submitted by an applicant.
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getLoansByApplicant(@PathVariable Long userId) {
        List<Map<String, Object>> loans = loanService.getLoansByApplicant(userId)
                .stream()
                .map(this::toLoanResponse)
                .toList();

        return ResponseEntity.ok(loans);
    }

    // Get loan action history.
    @GetMapping("/{loanId}/actions")
    public ResponseEntity<?> getLoanActions(@PathVariable Long loanId) {
        loanService.getLoanById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        List<Map<String, Object>> actions = loanActionService.getActionsForLoan(loanId)
                .stream()
                .map(this::toActionResponse)
                .toList();

        return ResponseEntity.ok(actions);
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
