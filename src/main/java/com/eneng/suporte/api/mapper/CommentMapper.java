package com.eneng.suporte.api.mapper;

import com.eneng.suporte.api.dto.CommentResponse;
import com.eneng.suporte.domain.model.Comment;
import org.springframework.stereotype.Component;

@Component
public class CommentMapper {

    public CommentResponse toResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getTicket().getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getUsername(),
                comment.getBody(),
                comment.isInternal(),
                comment.getCreatedAt()
        );
    }
}
