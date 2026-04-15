package com.eneng.suporte.service;

import com.eneng.suporte.domain.model.Comment;
import com.eneng.suporte.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CommentService {
    Comment adicionar(UUID ticketId, String body, boolean internal, User author);

    Page<Comment> listarPorTicket(UUID ticketId, Pageable pageable, User requester);
}
