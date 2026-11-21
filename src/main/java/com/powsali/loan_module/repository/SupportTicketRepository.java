package com.powsali.loan_module.repository;
import com.powsali.loan_module.entity.SupportTicket; import com.powsali.loan_module.entity.User;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface SupportTicketRepository extends JpaRepository<SupportTicket,Long> {
 List<SupportTicket> findByRequesterOrderByCreatedAtDesc(User requester);
 List<SupportTicket> findByAssigneeIsNullOrAssigneeOrderByUpdatedAtDesc(User assignee);
}
