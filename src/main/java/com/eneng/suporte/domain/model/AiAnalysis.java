package com.eneng.suporte.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_analysis", uniqueConstraints = @UniqueConstraint(name = "uk_ai_analysis_ticket", columnNames = "ticket_id"))
public class AiAnalysis {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(name = "suggested_category", length = 60)
    private String suggestedCategory;

    @Enumerated(EnumType.STRING)
    @Column(name = "suggested_priority", length = 20)
    private Priority suggestedPriority;

    @Column(name = "draft_reply", length = 4000)
    private String draftReply;

    @Column(precision = 4, scale = 3)
    private BigDecimal confidence;

    @Column(name = "model_name", length = 60)
    private String modelName;

    @Column(name = "prompt_hash", length = 100)
    private String promptHash;

    @Column(name = "fallback", nullable = false)
    private boolean fallback;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AiAnalysis() {
    }

    private AiAnalysis(Builder b) {
        this.id = b.id;
        this.ticket = b.ticket;
        this.suggestedCategory = b.suggestedCategory;
        this.suggestedPriority = b.suggestedPriority;
        this.draftReply = b.draftReply;
        this.confidence = b.confidence;
        this.modelName = b.modelName;
        this.promptHash = b.promptHash;
        this.fallback = b.fallback;
        this.createdAt = b.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public String getSuggestedCategory() {
        return suggestedCategory;
    }

    public void setSuggestedCategory(String suggestedCategory) {
        this.suggestedCategory = suggestedCategory;
    }

    public Priority getSuggestedPriority() {
        return suggestedPriority;
    }

    public void setSuggestedPriority(Priority suggestedPriority) {
        this.suggestedPriority = suggestedPriority;
    }

    public String getDraftReply() {
        return draftReply;
    }

    public void setDraftReply(String draftReply) {
        this.draftReply = draftReply;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getPromptHash() {
        return promptHash;
    }

    public void setPromptHash(String promptHash) {
        this.promptHash = promptHash;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static final class Builder {
        private UUID id;
        private Ticket ticket;
        private String suggestedCategory;
        private Priority suggestedPriority;
        private String draftReply;
        private BigDecimal confidence;
        private String modelName;
        private String promptHash;
        private boolean fallback;
        private Instant createdAt;

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder ticket(Ticket ticket) {
            this.ticket = ticket;
            return this;
        }

        public Builder suggestedCategory(String suggestedCategory) {
            this.suggestedCategory = suggestedCategory;
            return this;
        }

        public Builder suggestedPriority(Priority suggestedPriority) {
            this.suggestedPriority = suggestedPriority;
            return this;
        }

        public Builder draftReply(String draftReply) {
            this.draftReply = draftReply;
            return this;
        }

        public Builder confidence(BigDecimal confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder promptHash(String promptHash) {
            this.promptHash = promptHash;
            return this;
        }

        public Builder fallback(boolean fallback) {
            this.fallback = fallback;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public AiAnalysis build() {
            return new AiAnalysis(this);
        }
    }
}
