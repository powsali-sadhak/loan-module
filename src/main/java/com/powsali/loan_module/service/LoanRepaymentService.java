package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.LoanRecoveryHistoryResponse;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.LoanRepayment;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.LoanRepaymentRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanRepaymentService {

    private static final Set<Role> REPAYMENT_RECORDING_ROLES =
            Set.of(Role.DDO, Role.HOO, Role.ADMIN);

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final UserRepository userRepository;

    @Transactional
    public LoanRepayment recordRepayment(
            Long loanId,
            Long recordedById,
            Double amount,
            String remarks) {

        LoanApplication loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        User recorder = userRepository.findById(recordedById)
                .orElseThrow(() -> new IllegalArgumentException("Recording officer not found"));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("Repayments can only be recorded for an approved loan");
        }

        if (!REPAYMENT_RECORDING_ROLES.contains(recorder.getRole())) {
            throw new IllegalStateException("Only DDO, HOO or ADMIN users can record a repayment");
        }

        if (loan.getApplicant() != null
                && loan.getApplicant().getId().equals(recorder.getId())) {
            throw new IllegalStateException("A borrower cannot record their own loan repayment");
        }

        if (amount == null || amount.isNaN() || amount.isInfinite() || amount <= 0) {
            throw new IllegalArgumentException("Repayment amount must be greater than zero");
        }

        BigDecimal repaymentAmount = BigDecimal.valueOf(amount);
        BigDecimal principal = BigDecimal.valueOf(loan.getAmount());
        BigDecimal alreadyRecovered = repaymentRepository
                .findByLoanApplication_IdOrderByReceivedAtAsc(loanId)
                .stream()
                .map(item -> BigDecimal.valueOf(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal remaining = principal.subtract(alreadyRecovered);
        if (repaymentAmount.compareTo(remaining) > 0) {
            throw new IllegalArgumentException(
                    "Repayment exceeds the remaining principal of ₹" + remaining.toPlainString());
        }

        LoanRepayment repayment = LoanRepayment.builder()
                .loanApplication(loan)
                .recordedBy(recorder)
                .amount(amount)
                .remarks(remarks == null ? null : remarks.trim())
                .build();

        return repaymentRepository.save(repayment);
    }

    @Transactional(readOnly = true)
    public List<LoanRepayment> getRepayments(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new IllegalArgumentException("Loan not found");
        }
        return repaymentRepository.findByLoanApplication_IdOrderByReceivedAtAsc(loanId);
    }

    /**
     * Employees see recovery entries for their own loans.
     * DDO, HOO and ADMIN users see the department-wide recovery register.
     */
    @Transactional(readOnly = true)
    public List<LoanRecoveryHistoryResponse> getRecoveryHistory(Long viewerId) {
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<LoanRepayment> records;
        if (viewer.getRole() == Role.EMPLOYEE) {
            records = repaymentRepository
                    .findByLoanApplication_Applicant_IdOrderByReceivedAtDesc(viewerId);
        } else if (REPAYMENT_RECORDING_ROLES.contains(viewer.getRole())) {
            records = repaymentRepository.findAllByOrderByReceivedAtDesc();
        } else {
            throw new IllegalStateException(
                    "Recovery history is available to employees, DDO, HOO and ADMIN users");
        }

        return records.stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private LoanRecoveryHistoryResponse toHistoryResponse(LoanRepayment repayment) {
        LoanApplication loan = repayment.getLoanApplication();
        User applicant = loan.getApplicant();
        User recorder = repayment.getRecordedBy();

        return new LoanRecoveryHistoryResponse(
                repayment.getId(),
                loan.getId(),
                applicant.getId(),
                applicant.getName(),
                applicant.getRole().name(),
                applicant.getServiceGroup(),
                loan.getAmount(),
                repayment.getAmount(),
                repayment.getRemarks(),
                repayment.getReceivedAt(),
                recorder == null ? null : recorder.getId(),
                recorder == null ? null : recorder.getName());
    }
}
