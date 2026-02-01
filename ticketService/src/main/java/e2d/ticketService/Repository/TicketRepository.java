package e2d.ticketService.Repository;

import e2d.ticketService.Entity.Enum.TicketStatus;
import e2d.ticketService.Entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    List<Ticket> findByCreatedBy(String user);

    List<Ticket> findByAssignedTo(String user);

    List<Ticket> findByStatus(TicketStatus status);
}
