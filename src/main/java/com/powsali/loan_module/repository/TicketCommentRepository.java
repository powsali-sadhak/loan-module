package com.powsali.loan_module.repository;
import com.powsali.loan_module.entity.SupportTicket; import com.powsali.loan_module.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface TicketCommentRepository extends JpaRepository<TicketComment,Long> { List<TicketComment> findByTicketOrderByCreatedAtAsc(SupportTicket ticket); }
