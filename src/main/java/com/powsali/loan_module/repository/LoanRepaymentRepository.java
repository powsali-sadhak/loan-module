package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.LoanRepayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepaymentRepository extends JpaRepository<LoanRepayment, Long> {

    List<LoanRepayment> findByLoanApplication_Applicant_Id(Long applicantId);

    List<LoanRepayment> findByLoanApplication_Applicant_IdOrderByReceivedAtDesc(Long applicantId);

    List<LoanRepayment> findAllByOrderByReceivedAtDesc();

    List<LoanRepayment> findByLoanApplication_IdOrderByReceivedAtAsc(Long loanId);
}
