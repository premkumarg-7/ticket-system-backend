package e2d.ticketService.Exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(java.util.UUID id) {
        super("Ticket not found with id: " + id);
    }
}
