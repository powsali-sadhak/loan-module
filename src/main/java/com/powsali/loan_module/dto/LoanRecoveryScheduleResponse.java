package com.powsali.loan_module.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LoanRecoveryScheduleResponse(
        Long installmentId,
        Long loanId,
        Long applicantId,
        String applicantName,
        String applicantRole,
        String applicantGroup,
        Integer installmentNumber,
        BigDecimal recoveryAmount,
        BigDecimal paidAmount,
        LocalDate dueDate,
        LocalDateTime paidAt,
        String status) {
}
