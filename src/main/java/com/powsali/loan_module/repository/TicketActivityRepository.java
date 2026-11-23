package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.SupportTicket;
import com.powsali.loan_module.entity.TicketActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketActivityRepository extends JpaRepository<TicketActivity, Long> {

    List<TicketActivity> findByTicketOrderByCreatedAtAsc(SupportTicket ticket);
}
