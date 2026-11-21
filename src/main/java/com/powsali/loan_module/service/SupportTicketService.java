package com.powsali.loan_module.service;
import com.powsali.loan_module.dto.*;
import com.powsali.loan_module.entity.*;
import com.powsali.loan_module.entity.enums.*;
import com.powsali.loan_module.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime; import java.time.Year; import java.util.List;
@Service @RequiredArgsConstructor
public class SupportTicketService {
 private final SupportTicketRepository tickets; private final TicketCommentRepository comments; private final UserRepository users; private final LoanRepository loans;
 @Transactional public TicketResponseDTO create(User requester,TicketCreateRequestDTO req){
  if(req==null||req.getCategory()==null||req.getPriority()==null||blank(req.getSubject())||blank(req.getDescription())) throw new IllegalArgumentException("Category, priority, subject and description are required");
  if(req.getSubject().trim().length()>180||req.getDescription().trim().length()>5000) throw new IllegalArgumentException("Subject or description exceeds the allowed length");
  SupportTicket t=new SupportTicket(); t.setRequester(requester);t.setCategory(req.getCategory());t.setPriority(req.getPriority());t.setSubject(req.getSubject().trim());t.setDescription(req.getDescription().trim());t.setStatus(TicketStatus.OPEN);
  if(req.getLoanId()!=null){LoanApplication loan=loans.findById(req.getLoanId()).orElseThrow(()->new IllegalArgumentException("Loan application not found"));boolean owner=loan.getApplicant().getId().equals(requester.getId())||loan.getAppliedBy().getId().equals(requester.getId());if(!owner&&requester.getRole()!=Role.ADMIN)throw new AccessDeniedException("You can only link a loan you own or submitted");t.setLoanApplication(loan);}
  SupportTicket saved=tickets.save(t);saved.setTicketNumber("TKT-"+Year.now().getValue()+"-"+String.format("%06d",saved.getId()));return response(tickets.save(saved),requester);
 }
 @Transactional(readOnly=true) public List<TicketResponseDTO> mine(User u){return tickets.findByRequesterOrderByCreatedAtDesc(u).stream().map(t->response(t,u)).toList();}
 @Transactional(readOnly=true) public List<TicketResponseDTO> queue(User u){requireStaff(u);if(u.getRole()==Role.ADMIN)return tickets.findAll().stream().map(t->response(t,u)).toList();return tickets.findByAssigneeIsNullOrAssigneeOrderByUpdatedAtDesc(u).stream().map(t->response(t,u)).toList();}
 @Transactional(readOnly=true) public TicketResponseDTO get(Long id,User u){SupportTicket t=find(id);access(t,u);return response(t,u);}
 @Transactional public TicketResponseDTO assign(Long id,Long assigneeId,User actor){if(actor.getRole()!=Role.ADMIN)throw new AccessDeniedException("Only administrators can assign tickets");if(assigneeId==null)throw new IllegalArgumentException("Assignee ID is required");SupportTicket t=find(id);User assignee=users.findById(assigneeId).orElseThrow(()->new IllegalArgumentException("Assignee not found"));if(assignee.getRole()!=Role.SUPPORT)throw new IllegalArgumentException("Assignee must have SUPPORT role");if(t.getStatus()==TicketStatus.CLOSED)throw new IllegalStateException("Closed tickets cannot be assigned");t.setAssignee(assignee);if(t.getStatus()==TicketStatus.OPEN)t.setStatus(TicketStatus.ASSIGNED);return response(tickets.save(t),actor);}
 @Transactional public TicketResponseDTO status(Long id,TicketStatus next,String resolution,User actor){
  if(next==null)throw new IllegalArgumentException("Ticket status is required");SupportTicket t=find(id);access(t,actor);TicketStatus old=t.getStatus();
  if(!staff(actor)){if(t.getRequester().getId().equals(actor.getId())&&old==TicketStatus.RESOLVED&&next==TicketStatus.CLOSED){t.setStatus(next);t.setClosedAt(LocalDateTime.now());return response(tickets.save(t),actor);}throw new AccessDeniedException("Only support staff can change ticket status");}
  boolean valid=(old==TicketStatus.OPEN&&(next==TicketStatus.ASSIGNED||next==TicketStatus.IN_PROGRESS))||(old==TicketStatus.ASSIGNED&&(next==TicketStatus.IN_PROGRESS||next==TicketStatus.OPEN))||(old==TicketStatus.IN_PROGRESS&&next==TicketStatus.RESOLVED)||(old==TicketStatus.RESOLVED&&(next==TicketStatus.IN_PROGRESS||next==TicketStatus.CLOSED));
  if(!valid)throw new IllegalStateException("Unsupported status transition: "+old+" -> "+next);
  if(next==TicketStatus.RESOLVED&&blank(resolution))throw new IllegalArgumentException("Resolution summary is required");
  if(actor.getRole()==Role.SUPPORT&&t.getAssignee()==null)t.setAssignee(actor);t.setStatus(next);if(next==TicketStatus.RESOLVED){t.setResolution(resolution.trim());t.setResolvedAt(LocalDateTime.now());}if(next==TicketStatus.IN_PROGRESS){t.setResolvedAt(null);t.setClosedAt(null);}if(next==TicketStatus.CLOSED)t.setClosedAt(LocalDateTime.now());if(next==TicketStatus.OPEN)t.setAssignee(null);
  return response(tickets.save(t),actor);
 }
 @Transactional public TicketResponseDTO comment(Long id,TicketCommentRequestDTO req,User actor){if(req==null||blank(req.getBody())||req.getBody().trim().length()>3000)throw new IllegalArgumentException("Comment is required and must not exceed 3000 characters");SupportTicket t=find(id);access(t,actor);if(req.isInternalNote()&&!staff(actor))throw new AccessDeniedException("Only support staff can add internal notes");TicketComment c=new TicketComment();c.setTicket(t);c.setAuthor(actor);c.setBody(req.getBody().trim());c.setInternalNote(req.isInternalNote());comments.save(c);return response(t,actor);}
 private SupportTicket find(Long id){return tickets.findById(id).orElseThrow(()->new IllegalArgumentException("Ticket not found: "+id));}
 private void access(SupportTicket t,User u){if(u.getRole()==Role.ADMIN||t.getRequester().getId().equals(u.getId()))return;if(u.getRole()==Role.SUPPORT&&(t.getAssignee()==null||t.getAssignee().getId().equals(u.getId())))return;throw new AccessDeniedException("You are not allowed to access this ticket");}
 private TicketResponseDTO response(SupportTicket t,User viewer){List<TicketCommentResponseDTO> cs=comments.findByTicketOrderByCreatedAtAsc(t).stream().filter(c->!c.isInternalNote()||staff(viewer)).map(c->new TicketCommentResponseDTO(c.getId(),c.getAuthor().getName(),c.getBody(),c.isInternalNote(),c.getCreatedAt())).toList();return new TicketResponseDTO(t.getId(),t.getTicketNumber(),t.getRequester().getId(),t.getRequester().getName(),t.getLoanApplication()==null?null:t.getLoanApplication().getId(),t.getCategory(),t.getSubject(),t.getDescription(),t.getPriority(),t.getStatus(),t.getAssignee()==null?null:t.getAssignee().getId(),t.getAssignee()==null?null:t.getAssignee().getName(),t.getResolution(),t.getCreatedAt(),t.getUpdatedAt(),t.getResolvedAt(),t.getClosedAt(),cs);}
 private boolean staff(User u){return u.getRole()==Role.SUPPORT||u.getRole()==Role.ADMIN;}
 private void requireStaff(User u){if(!staff(u))throw new AccessDeniedException("Support role required");}
 private boolean blank(String s){return s==null||s.isBlank();}
}
