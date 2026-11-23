package com.powsali.loan_module.dto;

import lombok.Data;

@Data
public class LoanRepaymentRequestDTO {
    private Double amount;
    private String remarks;
}
