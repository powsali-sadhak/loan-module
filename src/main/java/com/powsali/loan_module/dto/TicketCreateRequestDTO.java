package com.powsali.loan_module.dto;
import com.powsali.loan_module.entity.enums.*;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class TicketCreateRequestDTO { private Long loanId; private TicketCategory category; private String subject; private String description; private TicketPriority priority; }
