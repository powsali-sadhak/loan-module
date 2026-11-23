package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.LoanEligibilityResponse;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.repository.LoanRepaymentRepository;
import com.powsali.loan_module.repository.LoanRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanEligibilityService {

    private static final BigDecimal ONE_LAKH = new BigDecimal("100000");
    private static final BigDecimal TEN_LAKH = new BigDecimal("1000000");
    private static final BigDecimal SEVEN_LAKH = new BigDecimal("700000");
    private static final BigDecimal FIVE_LAKH = new BigDecimal("500000");
    private static final BigDecimal RECOVERY_THRESHOLD = new BigDecimal("0.70");
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository repaymentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public LoanEligibilityResponse getEligibility(Long applicantId) {
        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new IllegalArgumentException("Applicant not found"));
        return getEligibility(applicant, null);
    }

    @Transactional(readOnly = true)
    public LoanEligibilityResponse getEligibility(User applicant, Long excludedLoanId) {
        EligibilitySnapshot snapshot = calculate(applicant, excludedLoanId);
        String message;

        if (snapshot.availableAmount().compareTo(ONE_LAKH) < 0) {
            if (!snapshot.recoveryThresholdMet()) {
                message = "Your loan entitlement is exhausted. At least 70% of the principal already availed must be recovered before another application can be submitted.";
            } else {
                message = "Your currently available loan entitlement is below the minimum application amount.";
            }
        } else {
            message = "You may apply for a loan between ₹1,00,000 and your available group limit.";
        }

        return new LoanEligibilityResponse(
                applicant.getId(),
                applicant.getUsername(),
                applicant.getRole().name(),
                applicant.getServiceGroup(),
                snapshot.financialYear(),
                snapshot.groupLimit(),
                snapshot.annualApplied(),
                snapshot.annualPrincipalRecovered(),
                snapshot.totalPrincipalApplied(),
                snapshot.totalPrincipalRecovered(),
                snapshot.outstandingPrincipal(),
                snapshot.availableAmount(),
                ONE_LAKH,
                snapshot.groupLimit().min(TEN_LAKH),
                snapshot.recoveryThresholdMet(),
                message);
    }

    @Transactional(readOnly = true)
    public void validateApplication(User applicant, Double requestedAmount, Long excludedLoanId) {
        if (requestedAmount == null || !Double.isFinite(requestedAmount)) {
            throw new IllegalArgumentException("A valid loan amount is required");
        }

        BigDecimal amount = BigDecimal.valueOf(requestedAmount);

        if (amount.compareTo(ONE_LAKH) < 0) {
            throw new IllegalArgumentException("The minimum loan application amount is ₹1,00,000");
        }

        EligibilitySnapshot snapshot = calculate(applicant, excludedLoanId);

        if (amount.compareTo(snapshot.groupLimit()) > 0) {
            throw new IllegalArgumentException(
                    "Your Group " + applicant.getServiceGroup()
                            + " maximum loan amount per application is ₹"
                            + snapshot.groupLimit().toPlainString());
        }

        if (amount.compareTo(snapshot.availableAmount()) > 0) {
            if (!snapshot.recoveryThresholdMet()) {
                throw new IllegalStateException(
                        "Your loan entitlement is exhausted. At least 70% of the principal already availed must be recovered before another application can be submitted.");
            }
            throw new IllegalStateException(
                    "The requested amount exceeds your currently available entitlement of ₹"
                            + snapshot.availableAmount().toPlainString());
        }
    }

    private EligibilitySnapshot calculate(User applicant, Long excludedLoanId) {
        String group = applicant.getServiceGroup() == null
                ? ""
                : applicant.getServiceGroup().trim().toUpperCase();

        BigDecimal groupLimit = switch (group) {
            case "A", "B" -> TEN_LAKH;
            case "C" -> SEVEN_LAKH;
            case "D" -> FIVE_LAKH;
            default -> throw new IllegalStateException(
                    "No loan entitlement is configured for service group '" + group
                            + "'. Set the applicant's group to A, B, C or D.");
        };

        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        int financialYearStart = today.getMonthValue() >= 4
                ? today.getYear()
                : today.getYear() - 1;
        LocalDateTime start = LocalDate.of(financialYearStart, 4, 1).atStartOfDay();
        LocalDateTime end = LocalDate.of(financialYearStart + 1, 4, 1).atStartOfDay();
        String financialYear = financialYearStart + "-" + (financialYearStart + 1);

        List<LoanApplication> allActiveLoans = loanRepository.findByApplicant(applicant)
                .stream()
                .filter(loan -> loan.getStatus() != LoanStatus.REJECTED)
                .filter(loan -> excludedLoanId == null || !excludedLoanId.equals(loan.getId()))
                .toList();

        List<LoanApplication> currentYearLoans = allActiveLoans.stream()
                .filter(loan -> loan.getCreatedAt() != null
                        && !loan.getCreatedAt().isBefore(start)
                        && loan.getCreatedAt().isBefore(end))
                .toList();

        BigDecimal annualApplied = sumPrincipal(currentYearLoans);
        BigDecimal totalApplied = sumPrincipal(allActiveLoans);

        Set<Long> allActiveLoanIds = allActiveLoans.stream()
                .map(LoanApplication::getId)
                .collect(Collectors.toSet());
        Set<Long> currentYearLoanIds = currentYearLoans.stream()
                .map(LoanApplication::getId)
                .collect(Collectors.toSet());

        List<com.powsali.loan_module.entity.LoanRepayment> allRepayments =
                repaymentRepository.findByLoanApplication_Applicant_Id(applicant.getId())
                        .stream()
                        .filter(repayment -> allActiveLoanIds.contains(
                                repayment.getLoanApplication().getId()))
                        .toList();

        BigDecimal totalRecovered = allRepayments.stream()
                .map(item -> money(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal annualRecovered = allRepayments.stream()
                .filter(item -> currentYearLoanIds.contains(
                        item.getLoanApplication().getId()))
                .map(item -> money(item.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRecovered.compareTo(totalApplied) > 0) {
            totalRecovered = totalApplied;
        }
        if (annualRecovered.compareTo(annualApplied) > 0) {
            annualRecovered = annualApplied;
        }

        BigDecimal outstanding = totalApplied.subtract(totalRecovered).max(BigDecimal.ZERO);
        BigDecimal annualOutstanding = annualApplied.subtract(annualRecovered).max(BigDecimal.ZERO);
        boolean recoveryThresholdMet = totalApplied.compareTo(BigDecimal.ZERO) == 0
                || totalRecovered.compareTo(totalApplied.multiply(RECOVERY_THRESHOLD)) >= 0;

        // Before 70% total principal recovery, repayments do not replenish entitlement.
        // Once met, both the current-year ceiling and all-time outstanding exposure
        // are checked so old loans continue to count after a financial-year change.
        BigDecimal annualConsumed = recoveryThresholdMet ? annualOutstanding : annualApplied;
        BigDecimal annualAvailable = groupLimit.subtract(annualConsumed).max(BigDecimal.ZERO);
        BigDecimal exposureAvailable = groupLimit.subtract(outstanding).max(BigDecimal.ZERO);

        // If lifetime borrowing has reached the group ceiling and the recovery
        // threshold is not met, block new requests even after the FY rolls over.
        BigDecimal available;
        if (totalApplied.compareTo(groupLimit) >= 0 && !recoveryThresholdMet) {
            available = BigDecimal.ZERO;
        } else {
            available = annualAvailable.min(exposureAvailable);
        }

        return new EligibilitySnapshot(
                financialYear,
                groupLimit,
                annualApplied,
                annualRecovered,
                totalApplied,
                totalRecovered,
                outstanding,
                available,
                recoveryThresholdMet);
    }

    private BigDecimal sumPrincipal(List<LoanApplication> loans) {
        return loans.stream()
                .map(loan -> money(loan.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal money(Double value) {
        return value == null ? BigDecimal.ZERO : BigDecimal.valueOf(value);
    }

    private record EligibilitySnapshot(
            String financialYear,
            BigDecimal groupLimit,
            BigDecimal annualApplied,
            BigDecimal annualPrincipalRecovered,
            BigDecimal totalPrincipalApplied,
            BigDecimal totalPrincipalRecovered,
            BigDecimal outstandingPrincipal,
            BigDecimal availableAmount,
            boolean recoveryThresholdMet) {
    }
}
