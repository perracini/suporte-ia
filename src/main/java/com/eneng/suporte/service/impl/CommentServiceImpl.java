package com.eneng.suporte.service.impl;

import com.eneng.suporte.domain.exception.BusinessRuleException;
import com.eneng.suporte.domain.exception.ResourceNotFoundException;
import com.eneng.suporte.domain.model.Comment;
import com.eneng.suporte.domain.model.Role;
import com.eneng.suporte.domain.model.Ticket;
import com.eneng.suporte.domain.model.User;
import com.eneng.suporte.repository.CommentRepository;
import com.eneng.suporte.repository.TicketRepository;
import com.eneng.suporte.service.CommentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    public CommentServiceImpl(CommentRepository commentRepository,
                              TicketRepository ticketRepository) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Comment adicionar(UUID ticketId, String body, boolean internal, User author) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        if (internal && author.getRole() == Role.CLIENT) {
            throw new BusinessRuleException("CLIENT nao pode criar comentario interno");
        }
        if (author.getRole() == Role.CLIENT &&
                !ticket.getCreatedBy().getId().equals(author.getId())) {
            throw new AccessDeniedException("Voce nao pode comentar neste ticket");
        }
        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(author)
                .body(body)
                .internal(internal)
                .build();
        return commentRepository.save(comment);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Comment> listarPorTicket(UUID ticketId, Pageable pageable, User requester) {
        ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", ticketId));
        if (requester.getRole() == Role.CLIENT) {
            return commentRepository.findByTicketIdAndInternalFalse(ticketId, pageable);
        }
        return commentRepository.findByTicketId(ticketId, pageable);
    }
}
