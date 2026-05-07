package e2d.ticketService.Service;

import e2d.ticketService.DTO.TicketDTO;
import e2d.ticketService.DTO.TicketEvent;
import e2d.ticketService.Entity.Enum.TicketEventType;
import e2d.ticketService.Entity.Enum.TicketStatus;
import e2d.ticketService.Entity.Ticket;
import e2d.ticketService.Client.AuthServiceClient;
import e2d.ticketService.Exception.TicketNotFoundException;
import e2d.ticketService.Exception.UserNotFoundException;
import e2d.ticketService.Mapper.TicketMapper;
import e2d.ticketService.Repository.TicketRepository;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final AuthServiceClient authServiceClient;

    @Autowired
    private KafkaTemplate<String, TicketEvent> kafkaTemplate;

    @Transactional
    public Ticket createTicket(TicketDTO ticketDTO, String creatorEmail) {
        Ticket ticket = ticketMapper.toEntity(ticketDTO);

        // SAVE FIRST - within transaction boundary
        Ticket savedTicket = ticketRepository.save(ticket);
        log.info("Ticket Created : {}", savedTicket);

        // CREATE EVENT WITH PROPER EMAIL
        TicketEvent event = new TicketEvent(
                savedTicket.getId(),
                savedTicket.getTitle(),
                creatorEmail,
                savedTicket.getAssignedTo(),
                null, // assignedToEmail null for CREATED events
                TicketEventType.TICKET_CREATED);

        // SEND EVENT AFTER SAVE - only if save succeeds
        try {
            kafkaTemplate.send("e2d-notification", event);
            log.info("Notification event sent for ticket {}", savedTicket.getId());
        } catch (Exception e) {
            log.error("Failed to send Kafka event for ticket {}: {}", savedTicket.getId(), e.getMessage());
            // Don't throw - ticket is already saved
        }

        return savedTicket;
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
                .orElseThrow(() -> new TicketNotFoundException(id));

        ticketMapper.updateEntityFromDto(ticketDTO, ticket);

        return ticketRepository.save(ticket);
    }

    @Transactional
    public Ticket updateAssignedTo(UUID id, String assignedToEmail, String creatorEmail) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        String oldAssignee = ticket.getAssignedTo();

        if (oldAssignee != null && oldAssignee.equals(assignedToEmail)) {
            log.info("No change in assignee for ticket {}", id);
            return ticket;
        }

        // Lookup assignee email via auth service REST call

        if (assignedToEmail != null) {
            if (!authServiceClient.checkUserEmailExist(assignedToEmail)) {
                throw new UserNotFoundException("Assignee not Found  " + assignedToEmail);
            }
        }

        ticket.setAssignedTo(assignedToEmail);
        Ticket updatedTicket = ticketRepository.save(ticket);

        TicketEvent event = new TicketEvent(
                updatedTicket.getId(),
                updatedTicket.getTitle(),
                creatorEmail,
                updatedTicket.getAssignedTo(),
                assignedToEmail,
                TicketEventType.TICKET_ASSIGNED);

        if (assignedToEmail != null) {
            try {
                kafkaTemplate.send("e2d-notification", event);
                log.info("Assignment event sent for ticket {} → {}", id, assignedToEmail);
            } catch (Exception e) {
                log.error("Failed to send assignment event for ticket {}: {}", id, e.getMessage());
            }
        }

        return updatedTicket;
    }

    @Transactional
    public void deleteTicket(UUID id) {
        if (!ticketRepository.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        ticketRepository.deleteById(id);
    }

    public List<Ticket> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status);
    }
}
