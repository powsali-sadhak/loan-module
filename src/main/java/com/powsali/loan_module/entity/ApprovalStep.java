package com.powsali.loan_module.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.powsali.loan_module.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "approval_steps",
       uniqueConstraints = @UniqueConstraint(name = "uk_workflow_step_order", columnNames = {"workflow_id", "step_order"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    @JsonIgnore
    private ApprovalWorkflow workflow;

    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "approver_role", nullable = false, length = 30)
    private Role approverRole;

    @Column(nullable = false, length = 100)
    private String label;
}
