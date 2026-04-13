package com.e2d.notification_service.Service;

import com.e2d.notification_service.DTO.TicketEvent;
import com.e2d.notification_service.DTO.TicketEventType;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Slf4j
@Service
public class EmailService {

    @Autowired
    public JavaMailSender mailSender;

    @Autowired
    public TemplateEngine templateEngine;

    public void sendMail(TicketEvent event) {

        Context context = new Context();
        context.setVariable("ticketId", event.ticketId());
        context.setVariable("ticketTitle", event.ticketTitle());
        context.setVariable("assignedTo", event.assignedTo());

        context.setVariable("type", event.eventType().name());

        String html = templateEngine.process("ticket-notification", context);

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            if (event.eventType() == TicketEventType.TICKET_CREATED) {
                helper.setTo(event.userEmail());
            } else {
                helper.setTo(event.assignedTo());
            }

            helper.setSubject(
                    event.eventType() == TicketEventType.TICKET_CREATED
                            ? "Ticket Created: " + event.ticketId()
                            : "Ticket Assigned: " + event.ticketId()
            );

            helper.setText(html, true);

            mailSender.send(message);
            log.info("Mail has been sent{}", message);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
