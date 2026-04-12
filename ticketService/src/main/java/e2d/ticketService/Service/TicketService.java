package e2d.ticketService.Service;

import e2d.ticketService.DTO.TicketDTO;
import e2d.ticketService.Entity.Enum.TicketStatus;
import e2d.ticketService.Entity.Ticket;
import e2d.ticketService.Mapper.TicketMapper;
import e2d.ticketService.Repository.TicketRepository;
import lombok.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;

    @Transactional
    public Ticket createTicket(TicketDTO ticketDTO) {
        Ticket ticket = ticketMapper.toEntity(ticketDTO);
        // Ensure status is set if missing, default to OPEN? Or let DB handle it?
        // Let's assume input might have it, if not, we can default in Entity or here.
        // For now, trusting mapper.
        return ticketRepository.save(ticket);
    }

    public Page<Ticket> getAllTickets(Pageable pageable) {
        return ticketRepository.findAll(pageable);
    }

    public List<Ticket> getAllTicketsList() {
        return ticketRepository.findAll();
    }

    public Optional<Ticket> getTicketById(UUID id) {
        return ticketRepository.findById(id);
    }

    @Transactional
    public Ticket updateTicket(UUID id, TicketDTO ticketDTO) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ticket not found with id: " + id));

        ticketMapper.updateEntityFromDto(ticketDTO, ticket);

        return ticketRepository.save(ticket);
    }

    @Transactional
    public void deleteTicket(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new RuntimeException("Ticket not found with id: " + id);
        }
        ticketRepository.deleteById(id);
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }
}
