package e2d.ticketService.DTO;

import e2d.ticketService.Entity.Enum.TicketPriority;
import e2d.ticketService.Entity.Enum.TicketStatus;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    private UUID id;

    private String title;

    private String description;

    private TicketStatus status;

    private TicketPriority priority;

    private String assignedTo;

    private List<String> attachments;

}
