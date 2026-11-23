package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.LoanDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanDocumentRepository extends JpaRepository<LoanDocument, Long> {

    List<LoanDocument> findByLoanApplication_IdOrderByUploadedAtDesc(Long loanId);

    Optional<LoanDocument> findByIdAndLoanApplication_Id(Long documentId, Long loanId);

    long countByLoanApplication_Id(Long loanId);
}
