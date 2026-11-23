package com.powsali.loan_module.dto;

import java.time.LocalDateTime;

/**
 * Safe row model for the recovery-history table.
 */
public record LoanRecoveryHistoryResponse(
        Long repaymentId,
        Long loanId,
        Long applicantId,
        String applicantName,
        String applicantRole,
        String applicantGroup,
        Double loanAmount,
        Double recoveryAmount,
        String remarks,
        LocalDateTime receivedAt,
        Long recordedById,
        String recordedByName) {
}
