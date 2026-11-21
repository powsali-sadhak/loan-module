package com.powsali.loan_module.entity;

import com.powsali.loan_module.entity.enums.LoanStatus;
import com.powsali.loan_module.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "loan_application")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "applicant_id")
    private User applicant;

    @ManyToOne
    @JoinColumn(name = "applied_by_id")
    private User appliedBy;

    private Double amount;
    private String purpose;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    // Kept for compatibility with the original API and records.
    private String currentOwner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private ApprovalWorkflow workflow;

    private Integer currentStepOrder;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Role currentApproverRole;

    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
