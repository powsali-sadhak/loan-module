package com.powsali.loan_module.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApprovalWorkflowRequestDTO {
    private String name;
    private String description;
    private List<ApprovalStepRequestDTO> steps;
    private boolean active;
}
