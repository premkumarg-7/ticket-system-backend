package com.e2d.notification_service.Listener;

import com.e2d.notification_service.DTO.TicketEvent;
import com.e2d.notification_service.Service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final EmailService emailService;

    @KafkaListener(topics = "e2d-notification", groupId = "notification-group")
    public void onTicketEvent(TicketEvent ticketEvent) {
        log.info("Received ticket event: {} for ticket {}", ticketEvent.eventType(), ticketEvent.ticketId());
        emailService.sendMail(ticketEvent);
    }
}
