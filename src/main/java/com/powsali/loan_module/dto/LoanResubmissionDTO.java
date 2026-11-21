
package com.powsali.loan_module.dto;

import lombok.Data;

@Data
public class LoanResubmissionDTO {

    private Long employeeId;

    private Double amount;

    private String purpose;

    private String remarks;
}
