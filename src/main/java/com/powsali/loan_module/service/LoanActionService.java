package com.powsali.loan_module.service;

import com.powsali.loan_module.entity.ApprovalStep;
import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.ActionType;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.LoanActionRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanActionService {

    private final LoanActionRepository actionRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    @Transactional
    public LoanAction recordAction(Long loanId, Long actionById, ActionType actionType, String remarks) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        User user = userRepository.findById(actionById)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (loan.getStatus() == LoanStatus.APPROVED || loan.getStatus() == LoanStatus.REJECTED) {
            throw new IllegalStateException("A completed loan application cannot be changed");
        }
        if (actionType == null) {
            throw new IllegalArgumentException("Action type is required");
        }
        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Remarks are required for workflow actions");
        }

        List<ApprovalStep> steps = loan.getWorkflow() == null
                ? List.of()
                : loan.getWorkflow().getSteps().stream()
                    .sorted(Comparator.comparing(ApprovalStep::getStepOrder)).toList();
        ApprovalStep currentStep = steps.stream()
                .filter(step -> step.getStepOrder().equals(loan.getCurrentStepOrder()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Current approval step is missing from the assigned workflow"));

        if (user.getRole() != currentStep.getApproverRole()) {
            throw new IllegalStateException("Only " + currentStep.getApproverRole()
                    + " can act on this application at the current step");
        }

        switch (actionType) {
            case FORWARDED -> {
                ApprovalStep nextStep = steps.stream()
                        .filter(step -> step.getStepOrder() > currentStep.getStepOrder())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("This is the final approval step; approve or reject the application"));
                loan.setCurrentStepOrder(nextStep.getStepOrder());
                loan.setCurrentApproverRole(nextStep.getApproverRole());
                loan.setCurrentOwner(nextStep.getApproverRole().name());
                loan.setStatus(LoanStatus.PENDING_APPROVAL);
            }
            case APPROVED -> {
                if (steps.stream().anyMatch(step -> step.getStepOrder() > currentStep.getStepOrder())) {
                    throw new IllegalStateException("Forward the application to the next configured approval step before final approval");
                }
                loan.setStatus(LoanStatus.APPROVED);
                loan.setCurrentOwner(null);
                loan.setCurrentApproverRole(null);
            }
            case REJECTED -> {
                loan.setStatus(LoanStatus.REJECTED);
                loan.setCurrentOwner(null);
                loan.setCurrentApproverRole(null);
            }
            case REVISION_REQUESTED -> {
                loan.setStatus(LoanStatus.REVISION_REQUESTED);
                loan.setCurrentOwner(Role.EMPLOYEE.name());
                loan.setCurrentApproverRole(Role.EMPLOYEE);
            }
        }

        loan.setRemarks(remarks.trim());
        loanRepository.save(loan);

        LoanAction action = LoanAction.builder()
                .loanApplication(loan)
                .actionBy(user)
                .actionType(actionType)
                .remarks(remarks.trim())
                .build();
        return actionRepository.save(action);
    }

    @Transactional(readOnly = true)
    public List<LoanAction> getActionsForLoan(Long loanId) {
        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
        return actionRepository.findByLoanApplicationId(loan);
    }
}
