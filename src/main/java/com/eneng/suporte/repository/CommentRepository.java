package com.eneng.suporte.repository;

import com.eneng.suporte.domain.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    Page<Comment> findByTicketId(UUID ticketId, Pageable pageable);

    Page<Comment> findByTicketIdAndInternalFalse(UUID ticketId, Pageable pageable);
}
