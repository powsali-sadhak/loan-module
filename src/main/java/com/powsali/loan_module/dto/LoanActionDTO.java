package com.powsali.loan_module.dto;

import com.powsali.loan_module.entity.enums.ActionType;
import lombok.Data;

@Data
public class LoanActionDTO {

    private Long loanId;
    private ActionType actionType;
    private String remarks;

}
