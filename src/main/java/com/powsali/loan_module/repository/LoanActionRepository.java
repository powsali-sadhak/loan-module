package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.LoanAction;
import com.powsali.loan_module.entity.LoanApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanActionRepository extends JpaRepository<LoanAction, Long> {

    List<LoanAction> findByLoanApplicationId(LoanApplication loanApplication);

}
