package com.powsali.loan_module.service;

import com.powsali.loan_module.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

/**
 * Sends optional WhatsApp notifications through a self-hosted Evolution API
 * instance. This adapter is intentionally isolated from loan workflow logic.
 */
@Service
@Slf4j
public class WhatsAppNotificationService {

    @Value("${app.whatsapp.enabled:false}")
    private boolean enabled;

    @Value("${app.whatsapp.base-url:http://localhost:8085}")
    private String baseUrl;

    @Value("${app.whatsapp.instance:loandesk}")
    private String instance;

    @Value("${app.whatsapp.api-key:}")
    private String apiKey;

    public void send(User recipient, String title, String message) {
        if (!enabled || recipient == null
                || !recipient.isWhatsappNotificationsEnabled()
                || recipient.getMobileNumber() == null
                || recipient.getMobileNumber().isBlank()
                || apiKey == null || apiKey.isBlank()) {
            return;
        }

        String number = normalizeNumber(recipient.getMobileNumber());
        if (number == null) {
            log.warn("WhatsApp delivery skipped: invalid mobile number for user id {}",
                    recipient.getId());
            return;
        }

        String text = title + "\n" + message
                + "\nPlease sign in to LoanDesk to view the details.";

        try {
            RestClient.create(baseUrl)
                    .post()
                    .uri("/message/sendText/{instance}", instance)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("number", number, "text", text))
                    .retrieve()
                    .toBodilessEntity();
            log.info("WhatsApp notification submitted for user id {}", recipient.getId());
        } catch (RestClientException | IllegalArgumentException exception) {
            // Do not fail or roll back loan/support operations when WhatsApp is unavailable.
            log.warn("WhatsApp delivery failed for user id {}: {}",
                    recipient.getId(), exception.getMessage());
        }
    }

    private String normalizeNumber(String mobileNumber) {
        String digits = mobileNumber.replaceAll("\\D", "");
        // Treat a ten-digit number as an Indian number for this India-based demo.
        if (digits.length() == 10) {
            digits = "91" + digits;
        }
        return digits.matches("[1-9][0-9]{7,14}") ? digits : null;
    }
}
