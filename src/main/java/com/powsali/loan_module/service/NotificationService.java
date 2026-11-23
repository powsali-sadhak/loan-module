package com.powsali.loan_module.service;

import com.powsali.loan_module.dto.NotificationResponseDTO;
import com.powsali.loan_module.entity.LoanApplication;
import com.powsali.loan_module.entity.SupportTicket;
import com.powsali.loan_module.entity.User;
import com.powsali.loan_module.entity.UserNotification;
import com.powsali.loan_module.entity.enums.ActionType;
import com.powsali.loan_module.entity.enums.Role;
import com.powsali.loan_module.repository.UserNotificationRepository;
import com.powsali.loan_module.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationRepository notifications;
    private final UserRepository users;

    /**
     * Notify the loan applicant and all DDO/HOO users when an application is
     * submitted or resubmitted. A linked map avoids duplicate delivery when
     * the applicant is themselves a DDO or HOO.
     */
    @Transactional
    public void loanSubmitted(LoanApplication loan, boolean modified) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        User applicant = loan.getApplicant();
        recipients.put(applicant.getId(), applicant);

        users.findByRole(Role.DDO).forEach(user -> recipients.put(user.getId(), user));
        users.findByRole(Role.HOO).forEach(user -> recipients.put(user.getId(), user));

        String loanNumber = loanNumber(loan.getId());
        String title = modified ? "Loan application modified" : "Loan application created";
        String message = modified
                ? loanNumber + " was resubmitted with updated details and is awaiting DDO review."
                : loanNumber + " was submitted successfully and is awaiting DDO review.";
        String type = modified ? "LOAN_MODIFIED" : "LOAN_CREATED";

        recipients.values().forEach(recipient -> create(
                recipient, title, message, type, "LOAN_APPLICATION", loan.getId()));
    }

    /**
     * Notify the borrower when the application changes state. When DDO forwards
     * the application, notify HOO users that it is ready for final review.
     */
    @Transactional
    public void loanAction(LoanApplication loan, ActionType actionType) {
        if (loan.getApplicant() != null) {
            String loanNumber = loanNumber(loan.getId());
            switch (actionType) {
                case FORWARDED -> {
                    create(loan.getApplicant(), "Loan application forwarded",
                            loanNumber + " has been forwarded by DDO and is awaiting HOO review.",
                            "LOAN_FORWARDED", "LOAN_APPLICATION", loan.getId());
                    users.findByRole(Role.HOO).forEach(user -> create(
                            user, "Loan awaiting HOO review",
                            loanNumber + " has been forwarded by DDO and is ready for your review.",
                            "LOAN_FORWARDED", "LOAN_APPLICATION", loan.getId()));
                }
                case APPROVED -> create(loan.getApplicant(), "Loan application approved",
                        loanNumber + " has been approved by HOO.",
                        "LOAN_APPROVED", "LOAN_APPLICATION", loan.getId());
                case REJECTED -> create(loan.getApplicant(), "Loan application rejected",
                        loanNumber + " has been rejected. Review the remarks in your application details.",
                        "LOAN_REJECTED", "LOAN_APPLICATION", loan.getId());
                case REVISION_REQUESTED -> create(loan.getApplicant(), "Changes requested",
                        loanNumber + " needs changes before it can continue through approval. Review the remarks.",
                        "LOAN_REVISION_REQUESTED", "LOAN_APPLICATION", loan.getId());
                case RESUBMITTED -> loanSubmitted(loan, true);
            }
        }
    }

    @Transactional
    public void supportTicketCreated(SupportTicket ticket) {
        User requester = ticket.getRequester();
        // Support and Admin are deliberately never recipients of support-ticket notifications.
        if (requester == null || requester.getRole() != Role.EMPLOYEE) {
            return;
        }
        create(requester, "Support ticket submitted",
                ticket.getTicketNumber() + " has been submitted successfully. Updates will appear here.",
                "SUPPORT_TICKET_CREATED", "SUPPORT_TICKET", ticket.getId());
    }

    @Transactional
    public void supportTicketUpdated(
            SupportTicket ticket,
            User actor,
            String title,
            String message,
            String type) {

        User requester = ticket.getRequester();
        // Only the employee who raised the ticket receives support notifications.
        // This excludes Support, Admin, DDO/HOO requesters and self-notifications.
        if (requester == null
                || requester.getRole() != Role.EMPLOYEE
                || actor == null
                || requester.getId().equals(actor.getId())) {
            return;
        }

        create(requester, title, message, type, "SUPPORT_TICKET", ticket.getId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> list(Long userId) {
        requireUserId(userId);
        return notifications.findTop50ByRecipient_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        requireUserId(userId);
        return notifications.countByRecipient_IdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponseDTO markRead(Long notificationId, Long userId) {
        requireUserId(userId);
        UserNotification notification = notifications.findByIdAndRecipient_Id(notificationId, userId)
                .orElseThrow(() -> new AccessDeniedException("Notification not found for this user"));
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = notifications.save(notification);
        }
        return toResponse(notification);
    }

    @Transactional
    public long markAllRead(Long userId) {
        requireUserId(userId);
        List<UserNotification> unread = notifications.findByRecipient_IdAndReadAtIsNull(userId);
        LocalDateTime now = LocalDateTime.now();
        unread.forEach(notification -> notification.setReadAt(now));
        notifications.saveAll(unread);
        return unread.size();
    }

    private void create(
            User recipient,
            String title,
            String message,
            String type,
            String referenceType,
            Long referenceId) {

        UserNotification notification = new UserNotification();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notifications.save(notification);
    }

    private NotificationResponseDTO toResponse(UserNotification notification) {
        return new NotificationResponseDTO(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.getReferenceType(),
                notification.getReferenceId(),
                notification.getCreatedAt(),
                notification.getReadAt(),
                notification.getReadAt() != null);
    }

    private void requireUserId(Long userId) {
        if (userId == null || !users.existsById(userId)) {
            throw new IllegalArgumentException("A valid user ID is required");
        }
    }

    private String loanNumber(Long id) {
        return "LN-" + String.format("%05d", id);
    }
}
