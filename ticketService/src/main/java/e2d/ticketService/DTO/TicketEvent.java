package e2d.ticketService.DTO;

import e2d.ticketService.Entity.Enum.TicketEventType;

import java.util.UUID;

public record TicketEvent(
        UUID ticketId,
        String ticketTitle,
        String userEmail,
        String assignedTo,
        String assignedToEmail,
        TicketEventType eventType
) {}
