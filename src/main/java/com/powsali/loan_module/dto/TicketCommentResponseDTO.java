package com.powsali.loan_module.dto;
import java.time.LocalDateTime;
public record TicketCommentResponseDTO(Long id,String authorName,String body,boolean internalNote,LocalDateTime createdAt) {}
