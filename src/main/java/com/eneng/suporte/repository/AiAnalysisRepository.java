package com.eneng.suporte.repository;

import com.eneng.suporte.domain.model.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {
    Optional<AiAnalysis> findByTicketId(UUID ticketId);
}
