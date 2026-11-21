package com.powsali.loan_module.entity;
import com.powsali.loan_module.entity.enums.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;
@Entity
@Table(name="support_tickets", indexes={@Index(name="idx_ticket_requester",columnList="requester_id"),@Index(name="idx_ticket_assignee_status",columnList="assignee_id,status")})
@Getter @Setter @NoArgsConstructor
public class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Column(nullable=false,unique=true,length=32) private String ticketNumber;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="requester_id",nullable=false) private User requester;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="loan_application_id") private LoanApplication loanApplication;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=40) private TicketCategory category;
 @Column(nullable=false,length=180) private String subject;
 @Column(nullable=false,length=5000) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TicketPriority priority;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private TicketStatus status=TicketStatus.OPEN;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assignee_id") private User assignee;
 @Column(length=3000) private String resolution;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
 private LocalDateTime resolvedAt;
 private LocalDateTime closedAt;
 @PrePersist void onCreate(){LocalDateTime now=LocalDateTime.now();createdAt=now;updatedAt=now;}
 @PreUpdate void onUpdate(){updatedAt=LocalDateTime.now();}
}
