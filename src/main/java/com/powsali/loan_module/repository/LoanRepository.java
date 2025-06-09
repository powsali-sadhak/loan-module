package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository  extends JpaRepository<LoanApplication, Long> {

    List<LoanApplication> findByApplicant(User applicant);
    List<LoanApplication> findByStatus(LoanStatus status);
    //List<LoanApplication> findByStatus(LoanStatus status);

}
