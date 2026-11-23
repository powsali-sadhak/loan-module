package com.powsali.loan_module.dto;

import com.powsali.loan_module.entity.enums.TicketCategory;
import com.powsali.loan_module.entity.enums.TicketPriority;
import com.powsali.loan_module.entity.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TicketResponseDTO(
        Long id,
        String ticketNumber,
        Long requesterId,
        String requesterName,
        Long loanId,
        TicketCategory category,
        String subject,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Long assigneeId,
        String assigneeName,
        String resolution,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt,
        LocalDateTime closedAt,
        List<TicketCommentResponseDTO> comments,
        List<TicketActivityResponseDTO> activities) {
}
