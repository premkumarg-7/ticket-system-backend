package com.e2d.notification_service.Controller;

import com.e2d.notification_service.DTO.TicketEvent;
import com.e2d.notification_service.Service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.RequestEntity;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Controller;

@Controller
public class NotificationController {

    @Autowired
    private EmailService emailService;


    @KafkaListener(topics = "e2d-notification", groupId = "notification-group")
    public void consumeNotificationEvent(TicketEvent ticketEvent) {
        emailService.sendMail(ticketEvent);
    }

}
