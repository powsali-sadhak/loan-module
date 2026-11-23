package com.powsali.loan_module.dto;

import java.math.BigDecimal;

/**
 * Read-only eligibility snapshot for the current Indian financial year.
 */
public record LoanEligibilityResponse(
        Long applicantId,
        String username,
        String role,
        String serviceGroup,
        String financialYear,
        BigDecimal annualLimit,
        BigDecimal annualApplied,
        BigDecimal principalRecovered,
        BigDecimal outstandingPrincipal,
        BigDecimal availableAmount,
        BigDecimal minimumApplicationAmount,
        BigDecimal maximumApplicationAmount,
        boolean recoveryThresholdMet,
        String message) {
}
