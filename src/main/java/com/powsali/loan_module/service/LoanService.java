package com.powsali.loan_module.service;

import com.powsali.loan_module.entity.ApprovalStep;
import com.powsali.loan_module.entity.ApprovalWorkflow;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.ApprovalWorkflowRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanService {

    private static final Set<Role> APPLICANT_ROLES =
            Set.of(Role.EMPLOYEE, Role.DDO, Role.HOO);

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final ApprovalWorkflowRepository workflowRepository;
    private final LoanEligibilityService eligibilityService;
    private final NotificationService notificationService;

    @Transactional
    public LoanApplication applyLoan(Long applicantId, Long appliedById, Double amount, String purpose) {
        User applicant = userRepository.findByIdForUpdate(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Applicant not found"));
        User appliedBy = userRepository.findById(appliedById)
                .orElseThrow(() -> new IllegalArgumentException("Applicant/submitter not found"));

        if (!APPLICANT_ROLES.contains(applicant.getRole())) {
            throw new IllegalStateException("Only EMPLOYEE, DDO and HOO users can apply for a loan");
        }

        if (!APPLICANT_ROLES.contains(appliedBy.getRole())) {
            throw new IllegalStateException("Only EMPLOYEE, DDO and HOO users can submit a loan application");
        }

        if ((appliedBy.getRole() == Role.EMPLOYEE || appliedBy.getRole() == Role.HOO)
                && !applicant.getId().equals(appliedBy.getId())) {
            throw new IllegalStateException(
                    appliedBy.getRole() + " users can only submit applications for themselves");
        }

        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Loan purpose is required");
        }

        // Applies equally to employee, DDO and HOO applicants using their service group.
        eligibilityService.validateApplication(applicant, amount, null);

        ApprovalWorkflow workflow = workflowRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No active approval workflow is configured"));

        List<ApprovalStep> orderedSteps = workflow.getSteps().stream()
                .sorted(Comparator.comparing(ApprovalStep::getStepOrder))
                .toList();

        if (orderedSteps.size() != 2
                || orderedSteps.get(0).getApproverRole() != Role.DDO
                || orderedSteps.get(1).getApproverRole() != Role.HOO) {
            throw new IllegalStateException(
                    "The active loan workflow must contain exactly two steps: DDO review followed by HOO final approval");
        }

        ApprovalStep firstStep = orderedSteps.get(0);

        LoanApplication loan = LoanApplication.builder()
                .applicant(applicant)
                .appliedBy(appliedBy)
                .amount(amount)
                .purpose(purpose.trim())
                .status(LoanStatus.PENDING_APPROVAL)
                .workflow(workflow)
                .currentStepOrder(firstStep.getStepOrder())
                .currentApproverRole(firstStep.getApproverRole())
                .currentOwner(firstStep.getApproverRole().name())
                .build();

        LoanApplication saved = loanRepository.save(loan);
        notificationService.loanSubmitted(saved, false);
        return saved;
    }

    public List<LoanApplication> getLoansByApplicant(Long applicantId) {
        User user = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return loanRepository.findByApplicant(user);
    }

    public Optional<LoanApplication> getLoanById(Long loanId) {
        return loanRepository.findById(loanId);
    }

    public LoanApplication updateLoan(LoanApplication loanApplication) {
        return loanRepository.save(loanApplication);
    }
}
