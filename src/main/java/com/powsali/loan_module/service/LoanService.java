package com.powsali.loan_module.service;

import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.ActionType;
import com.powsali.loan_module.entity.enums.LoanStatus;
//import com.powsali.loan_module.repository.LoanApplicationRepository;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserRepository userRepository;

    public LoanApplication applyLoan(Long applicantId, Long appliedById, Double amount, String purpose) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("Applicant not found"));

        User appliedBy = userRepository.findById(appliedById)
                .orElseThrow(() -> new RuntimeException("AppliedBy user not found"));

        LoanStatus status = appliedBy.getRole() == Role.DDO ? LoanStatus.PENDING_HOO : LoanStatus.PENDING_DDO;
        String currentOwner = appliedBy.getRole() == Role.DDO ? "HOO" : "DDO";

        LoanApplication loan = LoanApplication.builder()
                .applicant(applicant)
                .appliedBy(appliedBy)
                .amount(amount)
                .purpose(purpose)
                .status(status)
                .currentOwner(currentOwner)
                .build();

        return loanRepository.save(loan);
    }

    public List<LoanApplication> getLoansByApplicant(Long applicantId) {
        User user = userRepository.findById(applicantId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return loanRepository.findByApplicant(user);
    }

    public Optional<LoanApplication> getLoanById(Long loanId) {
        return loanRepository.findById(loanId);
    }

    public LoanApplication updateLoan(LoanApplication loanApplication) {
        return loanRepository.save(loanApplication);
    }
}