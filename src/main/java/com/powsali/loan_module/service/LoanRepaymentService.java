package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.LoanRecoveryHistoryResponse;
import com.powsali.loan_module.dto.LoanRecoveryScheduleResponse;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.LoanRecoveryInstallment;
import com.powsali.loan_module.entity.LoanRepayment;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.LoanRecoveryInstallmentRepository;
import com.powsali.loan_module.repository.LoanRepaymentRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class LoanRepaymentService {

    private static final Set<Role> REPAYMENT_RECORDING_ROLES =
            Set.of(Role.DDO, Role.HOO, Role.ADMIN);
    private static final Set<Role> BORROWER_ROLES =
            Set.of(Role.EMPLOYEE, Role.DDO, Role.HOO);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final LoanRecoveryInstallmentRepository installmentRepository;
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

        BigDecimal repaymentAmount = BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal principal = BigDecimal.valueOf(loan.getAmount()).setScale(2, RoundingMode.HALF_UP);
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
                .amount(repaymentAmount.doubleValue())
                .remarks(remarks == null ? null : remarks.trim())
                .build();

        LoanRepayment saved = repaymentRepository.save(repayment);
        allocatePaymentToInstallments(loanId, repaymentAmount, saved.getReceivedAt());
        return saved;
    }

    /**
     * The borrower chooses a fixed monthly principal recovery amount.
     * The backend creates a zero-interest schedule from the remaining principal,
     * with the first instalment due on the first day of the next month.
     */
    @Transactional
    public List<LoanRecoveryScheduleResponse> createRecoverySchedule(
            Long loanId,
            Long applicantId,
            BigDecimal monthlyAmount) {

        if (applicantId == null) {
            throw new IllegalArgumentException("Applicant ID is required");
        }
        if (monthlyAmount == null || monthlyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Monthly recovery amount must be greater than zero");
        }

        LoanApplication loan = loanRepository.findByIdForUpdate(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Applicant not found"));

        if (loan.getApplicant() == null || !loan.getApplicant().getId().equals(applicantId)) {
            throw new IllegalStateException("Only the borrower can choose the recovery amount for this loan");
        }
        if (!BORROWER_ROLES.contains(applicant.getRole())) {
            throw new IllegalStateException("Only an employee, DDO or HOO borrower can configure a recovery schedule");
        }
        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new IllegalStateException("A recovery schedule can only be created for an approved loan");
        }
        if (installmentRepository.existsByLoanApplication_Id(loanId)) {
            throw new IllegalStateException("A recovery schedule already exists for this loan");
        }

        BigDecimal principal = BigDecimal.valueOf(loan.getAmount()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal alreadyRecovered = repaymentRepository
                .findByLoanApplication_IdOrderByReceivedAtAsc(loanId)
                .stream()
                .map(item -> BigDecimal.valueOf(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal outstanding = principal.subtract(alreadyRecovered).setScale(2, RoundingMode.HALF_UP);
        BigDecimal selectedMonthlyAmount = monthlyAmount.setScale(2, RoundingMode.HALF_UP);

        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("This loan has no outstanding principal to schedule");
        }
        if (selectedMonthlyAmount.compareTo(outstanding) > 0) {
            throw new IllegalArgumentException(
                    "Monthly recovery amount cannot exceed the outstanding principal of ₹"
                            + outstanding.toPlainString());
        }

        LocalDate firstDueDate = LocalDate.now(BUSINESS_ZONE)
                .plusMonths(1)
                .withDayOfMonth(1);
        BigDecimal remaining = outstanding;
        int installmentNumber = 1;
        List<LoanRecoveryInstallment> installments = new ArrayList<>();

        while (remaining.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal installmentAmount = selectedMonthlyAmount.min(remaining);
            LocalDate dueDate = YearMonth.from(firstDueDate)
                    .plusMonths(installmentNumber - 1L)
                    .atDay(1);

            installments.add(LoanRecoveryInstallment.builder()
                    .loanApplication(loan)
                    .installmentNumber(installmentNumber)
                    .scheduledAmount(installmentAmount)
                    .paidAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .dueDate(dueDate)
                    .build());

            remaining = remaining.subtract(installmentAmount);
            installmentNumber++;
        }

        return installmentRepository.saveAll(installments).stream()
                .map(this::toScheduleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanRepayment> getRepayments(Long loanId) {
        if (!loanRepository.existsById(loanId)) {
            throw new IllegalArgumentException("Loan not found");
        }
        return repaymentRepository.findByLoanApplication_IdOrderByReceivedAtAsc(loanId);
    }

    @Transactional(readOnly = true)
    public List<LoanRecoveryScheduleResponse> getRecoverySchedule(Long viewerId) {
        User viewer = userRepository.findById(viewerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<LoanRecoveryInstallment> installments;
        if (viewer.getRole() == Role.EMPLOYEE) {
            installments = installmentRepository
                    .findByLoanApplication_Applicant_IdOrderByDueDateAscInstallmentNumberAsc(viewerId);
        } else if (REPAYMENT_RECORDING_ROLES.contains(viewer.getRole())) {
            installments = installmentRepository.findAllByOrderByDueDateAscInstallmentNumberAsc();
        } else {
            throw new IllegalStateException(
                    "Recovery schedules are available to employees, DDO, HOO and ADMIN users");
        }

        return installments.stream().map(this::toScheduleResponse).toList();
    }

    /**
     * Payments are allocated oldest-due-first. An advance payment can clear
     * future instalments; a partial payment remains marked as partial.
     */
    private void allocatePaymentToInstallments(
            Long loanId,
            BigDecimal payment,
            LocalDateTime paidAt) {

        BigDecimal unallocated = payment;
        List<LoanRecoveryInstallment> installments = installmentRepository
                .findByLoanApplication_IdOrderByDueDateAscInstallmentNumberAsc(loanId);

        for (LoanRecoveryInstallment installment : installments) {
            if (unallocated.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal scheduled = installment.getScheduledAmount();
            BigDecimal alreadyPaid = installment.getPaidAmount() == null
                    ? BigDecimal.ZERO : installment.getPaidAmount();
            BigDecimal balance = scheduled.subtract(alreadyPaid);

            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocation = unallocated.min(balance);
            BigDecimal updatedPaid = alreadyPaid.add(allocation).setScale(2, RoundingMode.HALF_UP);
            installment.setPaidAmount(updatedPaid);

            if (updatedPaid.compareTo(scheduled) >= 0 && installment.getPaidAt() == null) {
                installment.setPaidAt(paidAt == null ? LocalDateTime.now(BUSINESS_ZONE) : paidAt);
            }

            installmentRepository.save(installment);
            unallocated = unallocated.subtract(allocation);
        }
    }

    /**
     * Employees see their own recovery entries.
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

    private LoanRecoveryScheduleResponse toScheduleResponse(LoanRecoveryInstallment installment) {
        LoanApplication loan = installment.getLoanApplication();
        User applicant = loan.getApplicant();
        BigDecimal paid = installment.getPaidAmount() == null
                ? BigDecimal.ZERO : installment.getPaidAmount();
        String status = paid.compareTo(installment.getScheduledAmount()) >= 0
                ? "PAID"
                : paid.compareTo(BigDecimal.ZERO) > 0
                    ? "PARTIALLY_PAID"
                    : "UNPAID";

        return new LoanRecoveryScheduleResponse(
                installment.getId(),
                loan.getId(),
                applicant.getId(),
                applicant.getName(),
                applicant.getRole().name(),
                applicant.getServiceGroup(),
                installment.getInstallmentNumber(),
                installment.getScheduledAmount(),
                paid,
                installment.getDueDate(),
                installment.getPaidAt(),
                status);
    }
}
