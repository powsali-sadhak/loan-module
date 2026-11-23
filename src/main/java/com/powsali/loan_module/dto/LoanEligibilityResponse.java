package com.powsali.loan_module.dto;

import java.math.BigDecimal;

/**
 * Read-only eligibility snapshot. Annual usage is financial-year scoped;
 * recovery and outstanding principal are lifetime values for non-rejected loans.
 */
public record LoanEligibilityResponse(
        Long applicantId,
        String username,
        String role,
        String serviceGroup,
        String financialYear,
        BigDecimal annualLimit,
        BigDecimal annualApplied,
        BigDecimal annualPrincipalRecovered,
        BigDecimal totalPrincipalApplied,
        BigDecimal totalPrincipalRecovered,
        BigDecimal outstandingPrincipal,
        BigDecimal availableAmount,
        BigDecimal minimumApplicationAmount,
        BigDecimal maximumApplicationAmount,
        boolean recoveryThresholdMet,
        String message) {
}
