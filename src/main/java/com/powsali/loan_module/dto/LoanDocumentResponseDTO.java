package com.powsali.loan_module.dto;

import java.time.LocalDateTime;

public record LoanDocumentResponseDTO(
        Long documentId,
        Long loanId,
        String fileName,
        String contentType,
        long fileSize,
        String sha256,
        String uploadedBy,
        LocalDateTime uploadedAt) {
}
