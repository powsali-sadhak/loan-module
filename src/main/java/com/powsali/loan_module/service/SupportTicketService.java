package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.*;
import com.powsali.loan_module.entity.*;
import com.powsali.loan_module.entity.enums.*;
import com.powsali.loan_module.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SupportTicketService {

    private final SupportTicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketActivityRepository activities;
    private final UserRepository users;
    private final LoanRepository loans;
    private final NotificationService notificationService;

    @Transactional
    public TicketResponseDTO create(User requester, TicketCreateRequestDTO req) {
        if (req == null || req.getCategory() == null || req.getPriority() == null
                || blank(req.getSubject()) || blank(req.getDescription())) {
            throw new IllegalArgumentException(
                    "Category, priority, subject and description are required");
        }
        if (req.getSubject().trim().length() > 180
                || req.getDescription().trim().length() > 5000) {
            throw new IllegalArgumentException("Subject or description exceeds the allowed length");
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setRequester(requester);
        ticket.setCategory(req.getCategory());
        ticket.setPriority(req.getPriority());
        ticket.setSubject(req.getSubject().trim());
        ticket.setDescription(req.getDescription().trim());
        ticket.setStatus(TicketStatus.OPEN);

        if (req.getLoanId() != null) {
            LoanApplication loan = loans.findById(req.getLoanId())
                    .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));
            boolean owner = loan.getApplicant().getId().equals(requester.getId())
                    || (loan.getAppliedBy() != null
                    && loan.getAppliedBy().getId().equals(requester.getId()));
            if (!owner && requester.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("You can only link a loan you own or submitted");
            }
            ticket.setLoanApplication(loan);
        }

        ticket.setTicketNumber("TKT-" + Year.now().getValue() + "-"
                + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        SupportTicket saved = tickets.save(ticket);
        addActivity(saved, requester, "CREATED", null, TicketStatus.OPEN,
                "Support ticket submitted");
        notificationService.supportTicketCreated(saved);
        return response(saved, requester);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> mine(User user) {
        return tickets.findByRequesterOrderByCreatedAtDesc(user)
                .stream()
                .map(ticket -> response(ticket, user))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> queue(User user) {
        requireStaff(user);
        List<SupportTicket> rows = user.getRole() == Role.ADMIN
                ? tickets.findAll()
                : tickets.findByAssigneeIsNullOrAssigneeOrderByUpdatedAtDesc(user);

        return rows.stream().map(ticket -> response(ticket, user)).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO get(Long id, User user) {
        SupportTicket ticket = find(id);
        access(ticket, user);
        return response(ticket, user);
    }

    @Transactional
    public TicketResponseDTO assign(Long id, Long assigneeId, User actor) {
        if (actor.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only administrators can assign tickets");
        }
        if (assigneeId == null) {
            throw new IllegalArgumentException("Assignee ID is required");
        }

        SupportTicket ticket = find(id);
        User assignee = users.findById(assigneeId)
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found"));

        if (assignee.getRole() != Role.SUPPORT) {
            throw new IllegalArgumentException("Assignee must have SUPPORT role");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new IllegalStateException("Closed tickets cannot be assigned");
        }

        TicketStatus oldStatus = ticket.getStatus();
        User previousAssignee = ticket.getAssignee();
        ticket.setAssignee(assignee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.ASSIGNED);
        }

        SupportTicket saved = tickets.save(ticket);
        String event = previousAssignee == null ? "ASSIGNED" : "REASSIGNED";
        String message = previousAssignee == null
                ? "Assigned to " + assignee.getName()
                : "Reassigned from " + previousAssignee.getName() + " to " + assignee.getName();
        addActivity(saved, actor, event, oldStatus, saved.getStatus(), message);
        notificationService.supportTicketUpdated(
                saved, actor, "Support ticket assigned",
                saved.getTicketNumber() + " has been assigned to " + assignee.getName() + ".",
                "SUPPORT_TICKET_ASSIGNED");
        return response(saved, actor);
    }

    @Transactional
    public TicketResponseDTO status(Long id, TicketStatus next, String resolution, User actor) {
        if (next == null) {
            throw new IllegalArgumentException("Ticket status is required");
        }

        SupportTicket ticket = find(id);
        access(ticket, actor);
        TicketStatus old = ticket.getStatus();

        // A requester may only close a ticket after support has resolved it.
        if (!staff(actor)) {
            if (ticket.getRequester().getId().equals(actor.getId())
                    && old == TicketStatus.RESOLVED && next == TicketStatus.CLOSED) {
                ticket.setStatus(next);
                ticket.setClosedAt(LocalDateTime.now());
                SupportTicket saved = tickets.save(ticket);
                addActivity(saved, actor, "STATUS_CHANGED", old, next,
                        "Requester confirmed resolution and closed the ticket");
                return response(saved, actor);
            }
            throw new AccessDeniedException("Only support staff can change ticket status");
        }

        boolean valid = (old == TicketStatus.OPEN
                && (next == TicketStatus.ASSIGNED || next == TicketStatus.IN_PROGRESS))
                || (old == TicketStatus.ASSIGNED
                && (next == TicketStatus.IN_PROGRESS || next == TicketStatus.OPEN))
                || (old == TicketStatus.IN_PROGRESS && next == TicketStatus.RESOLVED)
                || (old == TicketStatus.RESOLVED
                && (next == TicketStatus.IN_PROGRESS || next == TicketStatus.CLOSED));

        if (!valid) {
            throw new IllegalStateException("Unsupported status transition: " + old + " -> " + next);
        }
        if (next == TicketStatus.RESOLVED && blank(resolution)) {
            throw new IllegalArgumentException("Resolution summary is required");
        }

        if (actor.getRole() == Role.SUPPORT
                && ticket.getAssignee() == null && next != TicketStatus.OPEN) {
            ticket.setAssignee(actor);
        }

        ticket.setStatus(next);
        if (next == TicketStatus.RESOLVED) {
            ticket.setResolution(resolution.trim());
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (next == TicketStatus.IN_PROGRESS) {
            ticket.setResolvedAt(null);
            ticket.setClosedAt(null);
        }
        if (next == TicketStatus.CLOSED) {
            ticket.setClosedAt(LocalDateTime.now());
        }
        if (next == TicketStatus.OPEN) {
            ticket.setAssignee(null);
        }

        SupportTicket saved = tickets.save(ticket);
        String message = next == TicketStatus.RESOLVED
                ? "Resolved: " + resolution.trim()
                : "Ticket status changed to " + next.name().replace('_', ' ');
        addActivity(saved, actor, "STATUS_CHANGED", old, next, message);
        notificationService.supportTicketUpdated(
                saved, actor,
                next == TicketStatus.RESOLVED ? "Support ticket resolved" : "Support ticket updated",
                saved.getTicketNumber() + " status changed to " + next.name().replace('_', ' ') + "."
                        + (next == TicketStatus.RESOLVED && saved.getResolution() != null
                        ? " " + saved.getResolution() : ""),
                "SUPPORT_TICKET_STATUS");
        return response(saved, actor);
    }

    @Transactional
    public TicketResponseDTO comment(Long id, TicketCommentRequestDTO req, User actor) {
        if (req == null || blank(req.getBody()) || req.getBody().trim().length() > 3000) {
            throw new IllegalArgumentException("Comment is required and must not exceed 3000 characters");
        }

        SupportTicket ticket = find(id);
        access(ticket, actor);

        if (req.isInternalNote() && !staff(actor)) {
            throw new AccessDeniedException("Only support staff can add internal notes");
        }

        TicketComment comment = new TicketComment();
        comment.setTicket(ticket);
        comment.setAuthor(actor);
        comment.setBody(req.getBody().trim());
        comment.setInternalNote(req.isInternalNote());
        comments.save(comment);

        if (!req.isInternalNote()) {
            notificationService.supportTicketUpdated(
                    ticket, actor, "New support ticket reply",
                    ticket.getTicketNumber() + " has a new reply from " + actor.getName() + ".",
                    "SUPPORT_TICKET_REPLY");
        }

        return response(ticket, actor);
    }

    private void addActivity(
            SupportTicket ticket,
            User actor,
            String eventType,
            TicketStatus fromStatus,
            TicketStatus toStatus,
            String message) {

        TicketActivity activity = new TicketActivity();
        activity.setTicket(ticket);
        activity.setActor(actor);
        activity.setEventType(eventType);
        activity.setFromStatus(fromStatus);
        activity.setToStatus(toStatus);
        activity.setMessage(message);
        activities.save(activity);
    }

    private SupportTicket find(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found: " + id));
    }

    private void access(SupportTicket ticket, User user) {
        if (user.getRole() == Role.ADMIN
                || ticket.getRequester().getId().equals(user.getId())) {
            return;
        }
        if (user.getRole() == Role.SUPPORT
                && (ticket.getAssignee() == null
                || ticket.getAssignee().getId().equals(user.getId()))) {
            return;
        }
        throw new AccessDeniedException("You are not allowed to access this ticket");
    }

    private TicketResponseDTO response(SupportTicket ticket, User viewer) {
        List<TicketCommentResponseDTO> ticketComments =
                comments.findByTicketOrderByCreatedAtAsc(ticket)
                        .stream()
                        .filter(comment -> !comment.isInternalNote() || staff(viewer))
                        .map(comment -> new TicketCommentResponseDTO(
                                comment.getId(),
                                comment.getAuthor().getName(),
                                comment.getBody(),
                                comment.isInternalNote(),
                                comment.getCreatedAt()))
                        .toList();

        List<TicketActivityResponseDTO> ticketActivities =
                activities.findByTicketOrderByCreatedAtAsc(ticket)
                        .stream()
                        .map(activity -> new TicketActivityResponseDTO(
                                activity.getId(),
                                activity.getEventType(),
                                activity.getFromStatus(),
                                activity.getToStatus(),
                                activity.getMessage(),
                                activity.getActor().getName(),
                                activity.getCreatedAt()))
                        .toList();

        return new TicketResponseDTO(
                ticket.getId(),
                ticket.getTicketNumber(),
                ticket.getRequester().getId(),
                ticket.getRequester().getName(),
                ticket.getLoanApplication() == null ? null : ticket.getLoanApplication().getId(),
                ticket.getCategory(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignee() == null ? null : ticket.getAssignee().getId(),
                ticket.getAssignee() == null ? null : ticket.getAssignee().getName(),
                ticket.getResolution(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt(),
                ticket.getClosedAt(),
                ticketComments,
                ticketActivities);
    }

    private boolean staff(User user) {
        return user.getRole() == Role.SUPPORT || user.getRole() == Role.ADMIN;
    }

    private void requireStaff(User user) {
        if (!staff(user)) {
            throw new AccessDeniedException("Support role required");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
