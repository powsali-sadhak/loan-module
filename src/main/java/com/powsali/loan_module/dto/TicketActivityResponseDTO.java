package com.powsali.loan_module.dto;

import com.powsali.loan_module.entity.enums.TicketStatus;

import java.time.LocalDateTime;

public record TicketActivityResponseDTO(
        Long id,
        String eventType,
        TicketStatus fromStatus,
        TicketStatus toStatus,
        String message,
        String actorName,
        LocalDateTime createdAt) {
}
