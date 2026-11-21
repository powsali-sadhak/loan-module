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

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;
    private final ApprovalWorkflowRepository workflowRepository;

    @Transactional
    public LoanApplication applyLoan(Long applicantId, Long appliedById, Double amount, String purpose) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Applicant not found"));
        User appliedBy = userRepository.findById(appliedById)
                .orElseThrow(() -> new IllegalArgumentException("Authenticated applicant/submitter not found"));

        if (appliedBy.getRole() == Role.EMPLOYEE && !applicant.getId().equals(appliedBy.getId())) {
            throw new IllegalStateException("Employees can only submit loan applications for themselves");
        }
        if (appliedBy.getRole() != Role.EMPLOYEE && appliedBy.getRole() != Role.DDO) {
            throw new IllegalStateException("Only employees and DDO users can submit loan applications");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Loan amount must be greater than zero");
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Loan purpose is required");
        }

        ApprovalWorkflow workflow = workflowRepository.findFirstByActiveTrueOrderByIdAsc()
                .orElseThrow(() -> new IllegalStateException("No active approval workflow is configured"));
        ApprovalStep firstStep = workflow.getSteps().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("The active workflow has no approval steps"));

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

        return loanRepository.save(loan);
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
