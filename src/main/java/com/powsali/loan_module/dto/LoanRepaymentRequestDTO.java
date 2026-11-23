package com.powsali.loan_module.dto;

import lombok.Data;

@Data
public class LoanRepaymentRequestDTO {
    private Long recordedById;
    private Double amount;
    private String remarks;
}
