package com.powsali.loan_module.controller;

import com.powsali.loan_module.dto.ApprovalWorkflowRequestDTO;
import com.powsali.loan_module.entity.ApprovalWorkflow;
import com.powsali.loan_module.service.ApprovalWorkflowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/workflows")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ApprovalWorkflowController {
    private final ApprovalWorkflowService workflowService;

    @GetMapping
    public List<ApprovalWorkflow> getWorkflows() {
        return workflowService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApprovalWorkflow createWorkflow(@RequestBody ApprovalWorkflowRequestDTO request) {
        return workflowService.create(request);
    }

    @PutMapping("/{workflowId}")
    public ApprovalWorkflow updateWorkflow(@PathVariable Long workflowId,
                                            @RequestBody ApprovalWorkflowRequestDTO request) {
        return workflowService.update(workflowId, request);
    }

    @PostMapping("/{workflowId}/activate")
    public ApprovalWorkflow activateWorkflow(@PathVariable Long workflowId) {
        return workflowService.activate(workflowId);
    }
}
