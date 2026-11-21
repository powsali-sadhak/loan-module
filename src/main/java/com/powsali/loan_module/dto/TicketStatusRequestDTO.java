package com.powsali.loan_module.dto;
import com.powsali.loan_module.entity.enums.TicketStatus;
import lombok.Getter; import lombok.Setter;
@Getter @Setter
public class TicketStatusRequestDTO { private TicketStatus status; private String resolution; }
