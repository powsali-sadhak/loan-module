package com.powsali.loan_module.dto;

import com.powsali.loan_module.entity.enums.Role;
import lombok.Data;

@Data
public class ApprovalStepRequestDTO {
    private Integer stepOrder;
    private Role approverRole;
    private String label;
}
