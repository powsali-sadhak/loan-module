package com.powsali.loan_module.repository;

import com.powsali.loan_module.entity.UserNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findTop50ByRecipient_IdOrderByCreatedAtDesc(Long recipientId);

    List<UserNotification> findByRecipient_IdAndReadAtIsNull(Long recipientId);

    long countByRecipient_IdAndReadAtIsNull(Long recipientId);

    Optional<UserNotification> findByIdAndRecipient_Id(Long id, Long recipientId);
}
