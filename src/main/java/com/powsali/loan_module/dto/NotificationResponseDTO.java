package com.powsali.loan_module.dto;

import java.time.LocalDateTime;

public record NotificationResponseDTO(
        Long id,
        String title,
        String message,
        String type,
        String referenceType,
        Long referenceId,
        LocalDateTime createdAt,
        LocalDateTime readAt,
        boolean read) {
}
