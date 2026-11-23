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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanActionService {

    private static final Set<Role> APPLICANT_ROLES =
            Set.of(Role.EMPLOYEE, Role.DDO, Role.HOO);

    private final LoanActionRepository actionRepository;
    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final LoanEligibilityService eligibilityService;

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

        // Officer applicants must be reviewed by a different officer at every step.
        if (loan.getApplicant() != null && loan.getApplicant().getId().equals(user.getId())) {
            throw new IllegalStateException("Applicants cannot approve, forward, reject or request revision on their own loan");
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
                if (currentStep.getApproverRole() != Role.DDO) {
                    throw new IllegalStateException("Only the DDO stage can forward an application to HOO");
                }
                ApprovalStep nextStep = steps.stream()
                        .filter(step -> step.getStepOrder() > currentStep.getStepOrder())
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("This is the final approval step; approve or reject the application"));
                if (nextStep.getApproverRole() != Role.HOO) {
                    throw new IllegalStateException("The next approval step must be HOO");
                }
                loan.setCurrentStepOrder(nextStep.getStepOrder());
                loan.setCurrentApproverRole(nextStep.getApproverRole());
                loan.setCurrentOwner(nextStep.getApproverRole().name());
                loan.setStatus(LoanStatus.PENDING_APPROVAL);
            }
            case APPROVED -> {
                if (currentStep.getApproverRole() != Role.HOO) {
                    throw new IllegalStateException("Only HOO can give final approval");
                }
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
                loan.setCurrentOwner(loan.getApplicant().getRole().name());
                loan.setCurrentApproverRole(loan.getApplicant().getRole());
            }
            case RESUBMITTED -> throw new IllegalStateException("Use the resubmit endpoint to resubmit an application");
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
        return actionRepository.findByLoanApplication_IdOrderByActionTimeAsc(loanId);
    }

    @Transactional
    public LoanApplication resubmitLoan(
            Long loanId,
            Long applicantId,
            Double amount,
            String purpose,
            String remarks) {

        LoanApplication loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Applicant not found"));

        if (!APPLICANT_ROLES.contains(applicant.getRole())) {
            throw new IllegalStateException("Only EMPLOYEE, DDO or HOO applicants can resubmit a loan application");
        }

        if (loan.getApplicant() == null || !loan.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("You can only resubmit your own loan application");
        }

        if (loan.getStatus() != LoanStatus.REVISION_REQUESTED) {
            throw new IllegalStateException("This application is not awaiting revision");
        }

        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Loan purpose is required");
        }

        if (remarks == null || remarks.isBlank()) {
            throw new IllegalArgumentException("Resubmission remarks are required");
        }

        // Exclude this application's previous reservation while validating the revised amount.
        eligibilityService.validateApplication(applicant, amount, loanId);

        if (loan.getWorkflow() == null) {
            throw new IllegalStateException("The assigned approval workflow is missing");
        }

        List<ApprovalStep> steps = loan.getWorkflow().getSteps()
                .stream()
                .sorted(Comparator.comparing(ApprovalStep::getStepOrder))
                .toList();

        ApprovalStep firstStep = steps.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The assigned workflow has no approval steps"));

        if (firstStep.getApproverRole() != Role.DDO) {
            throw new IllegalStateException("The first approval step must be DDO");
        }

        loan.setAmount(amount);
        loan.setPurpose(purpose.trim());
        loan.setRemarks(remarks.trim());
        loan.setStatus(LoanStatus.PENDING_APPROVAL);
        loan.setCurrentStepOrder(firstStep.getStepOrder());
        loan.setCurrentApproverRole(firstStep.getApproverRole());
        loan.setCurrentOwner(firstStep.getApproverRole().name());

        loanRepository.save(loan);

        LoanAction action = LoanAction.builder()
                .loanApplication(loan)
                .actionBy(applicant)
                .actionType(ActionType.RESUBMITTED)
                .remarks(remarks.trim())
                .build();

        actionRepository.save(action);
        return loan;
    }
}
