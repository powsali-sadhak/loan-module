package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.LoanRecoveryInstallment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRecoveryInstallmentRepository extends JpaRepository<LoanRecoveryInstallment, Long> {

    boolean existsByLoanApplication_Id(Long loanId);

    List<LoanRecoveryInstallment> findByLoanApplication_IdOrderByDueDateAscInstallmentNumberAsc(Long loanId);

    List<LoanRecoveryInstallment> findByLoanApplication_Applicant_IdOrderByDueDateAscInstallmentNumberAsc(Long applicantId);

    List<LoanRecoveryInstallment> findAllByOrderByDueDateAscInstallmentNumberAsc();
}
