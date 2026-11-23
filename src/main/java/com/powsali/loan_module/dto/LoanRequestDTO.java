package com.powsali.loan_module.dto;

import lombok.Data;

@Data
public class LoanRequestDTO {

    private Long applicantId;
    private Double amount;
    private String purpose;

}


