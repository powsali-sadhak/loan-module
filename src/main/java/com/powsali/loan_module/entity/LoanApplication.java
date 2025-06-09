package com.powsali.loan_module.entity;

import com.powsali.loan_module.entity.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Employee who is requesting the loan
    @ManyToOne
    @JoinColumn(name = "applied_by_id")
    private User appliedBy;
    private Double amount;
    private String purpose;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    // Who needs to act on this loan now
    private String currentOwner;
    private String remarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate(){
        updatedAt = LocalDateTime.now();
    }

}
