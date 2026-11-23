package com.powsali.loan_module.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class LoanRecoveryScheduleRequestDTO {
    private BigDecimal monthlyAmount;
}
