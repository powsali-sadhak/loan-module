package com.powsali.loan_module.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Data
@Table(
    name = "loan_recovery_installments",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_loan_recovery_installment",
        columnNames = {"loan_id", "installment_number"}
    )
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanRecoveryInstallment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private LoanApplication loanApplication;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "scheduled_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal scheduledAmount;

    @Builder.Default
    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}
