package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.LoanActionDTO;
import com.powsali.loan_module.dto.LoanRequestDTO;
import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.service.LoanActionService;
import com.powsali.loan_module.service.LoanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;
    private final LoanActionService loanActionService;

    // 1️⃣ Apply for a Loan
    @PostMapping("/apply")
    public ResponseEntity<LoanApplication> applyLoan(@RequestBody LoanRequestDTO request) {
        LoanApplication loan = loanService.applyLoan(
                request.getApplicantId(),
                request.getAppliedById(),
                request.getAmount(),
                request.getPurpose()
        );
        return ResponseEntity.ok(loan);
    }

        // 2️⃣ Take Action on a Loan (Forward / Approve / Reject / Revise)
        @PostMapping("/action")
        public ResponseEntity<LoanAction> takeAction(@RequestBody LoanActionDTO actionDTO) {
            LoanAction action = loanActionService.recordAction(
                    actionDTO.getLoanId(),
                    actionDTO.getUserId(),
                    actionDTO.getActionType(),
                    actionDTO.getRemarks()
            );
            return ResponseEntity.ok(action);
        }

    // 3️⃣ View a loan application
    @GetMapping("/{loanId}")
    public ResponseEntity<LoanApplication> getLoan(@PathVariable Long loanId){
        return loanService.getLoanById(loanId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // 4️⃣ View all loans by an employee
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LoanApplication>> getLoansByUser(@PathVariable Long userId){
        return ResponseEntity.ok(loanService.getLoansByApplicant(userId));
    }

    // 5️⃣ View action history for a loan
    @GetMapping("/{loanId}/actions")
    public ResponseEntity<List<LoanAction>> getLoanActions(@PathVariable Long loanId){
        return ResponseEntity.ok(loanActionService.getActionsForLoan(loanId));
    }

    }

