package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.ApprovalWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalWorkflowRepository extends JpaRepository<ApprovalWorkflow, Long> {
    Optional<ApprovalWorkflow> findFirstByActiveTrueOrderByIdAsc();
    List<ApprovalWorkflow> findAllByOrderByIdAsc();
    boolean existsByNameIgnoreCase(String name);
}
