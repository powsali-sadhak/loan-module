package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.ApprovalStepRequestDTO;
import com.powsali.loan_module.dto.ApprovalWorkflowRequestDTO;
import com.powsali.loan_module.entity.ApprovalStep;
import com.powsali.loan_module.entity.ApprovalWorkflow;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.ApprovalWorkflowRepository;
import com.powsali.loan_module.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ApprovalWorkflowService {
    private final ApprovalWorkflowRepository workflowRepository;
    private final LoanRepository loanRepository;

    @Transactional(readOnly = true)
    public List<ApprovalWorkflow> getAll() {
        return workflowRepository.findAllByOrderByIdAsc();
    }

    @Transactional
    public ApprovalWorkflow create(ApprovalWorkflowRequestDTO request) {
        validate(request);
        if (workflowRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new IllegalArgumentException("A workflow with this name already exists");
        }
        ApprovalWorkflow workflow = ApprovalWorkflow.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(false)
                .build();
        workflow.replaceSteps(toSteps(request.getSteps()));
        ApprovalWorkflow saved = workflowRepository.save(workflow);
        return request.isActive() ? activate(saved.getId()) : saved;
    }

    @Transactional
    public ApprovalWorkflow update(Long id, ApprovalWorkflowRequestDTO request) {
        validate(request);
        ApprovalWorkflow workflow = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        if (loanRepository.existsByWorkflowId(id)) {
            throw new IllegalStateException("This workflow is already assigned to loan applications. Create a new workflow version instead.");
        }
        workflowRepository.findAllByOrderByIdAsc().stream()
                .filter(other -> !other.getId().equals(id))
                .filter(other -> other.getName().equalsIgnoreCase(request.getName().trim()))
                .findAny()
                .ifPresent(other -> { throw new IllegalArgumentException("A workflow with this name already exists"); });
        workflow.setName(request.getName().trim());
        workflow.setDescription(request.getDescription());
        workflow.replaceSteps(toSteps(request.getSteps()));
        ApprovalWorkflow saved = workflowRepository.save(workflow);
        return request.isActive() ? activate(saved.getId()) : saved;
    }

    @Transactional
    public ApprovalWorkflow activate(Long id) {
        ApprovalWorkflow selected = workflowRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + id));
        workflowRepository.findAll().forEach(workflow -> workflow.setActive(workflow.getId().equals(id)));
        return workflowRepository.save(selected);
    }

    private void validate(ApprovalWorkflowRequestDTO request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Workflow name is required");
        }
        if (request.getSteps() == null || request.getSteps().isEmpty()) {
            throw new IllegalArgumentException("At least one approval step is required");
        }
        List<ApprovalStepRequestDTO> ordered = request.getSteps().stream()
                .sorted(Comparator.comparing(ApprovalStepRequestDTO::getStepOrder,
                        Comparator.nullsFirst(Integer::compareTo))).toList();
        for (int i = 0; i < ordered.size(); i++) {
            ApprovalStepRequestDTO step = ordered.get(i);
            if (step.getStepOrder() == null || step.getStepOrder() != i + 1) {
                throw new IllegalArgumentException("Approval step orders must be sequential starting at 1");
            }
            if (step.getApproverRole() == null || step.getApproverRole() == Role.EMPLOYEE
                    || step.getApproverRole() == Role.ADMIN || step.getApproverRole() == Role.SUPPORT) {
                throw new IllegalArgumentException("Each approval step must have an approver role such as DDO or HOO");
            }
            if (step.getLabel() == null || step.getLabel().isBlank()) {
                throw new IllegalArgumentException("Approval step label is required");
            }
        }
    }

    private List<ApprovalStep> toSteps(List<ApprovalStepRequestDTO> requests) {
        return requests.stream().map(step -> ApprovalStep.builder()
                .stepOrder(step.getStepOrder())
                .approverRole(step.getApproverRole())
                .label(step.getLabel().trim())
                .build()).toList();
    }
}
