package com.powsali.loan_module.entity;

import com.powsali.loan_module.entity.enums.ActionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "loan_actions")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    private LoanApplication loanApplication;

    @ManyToOne
    @JoinColumn(name = "action_by")
    private User actionBy;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private String remarks;

    private LocalDateTime actionTime;

    @PrePersist
    protected void onCreate() {
        actionTime = LocalDateTime.now();
    }
}