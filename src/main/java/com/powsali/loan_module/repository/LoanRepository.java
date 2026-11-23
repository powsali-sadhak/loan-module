package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.enums.LoanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<LoanApplication, Long> {
    List<LoanApplication> findByApplicant(User applicant);
    List<LoanApplication> findByStatus(LoanStatus status);
    boolean existsByWorkflowId(Long workflowId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from LoanApplication l where l.id = :loanId")
    Optional<LoanApplication> findByIdForUpdate(@Param("loanId") Long loanId);
}
