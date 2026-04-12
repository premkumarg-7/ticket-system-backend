package com.e2d.notification_service.DTO;

import java.util.UUID;

public record TicketEvent(
        UUID ticketId,
        String ticketTitle,
        String userEmail,
        String assignedTo,
        TicketEventType eventType
) {}
