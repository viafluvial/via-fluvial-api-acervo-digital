package br.com.viafluvial.acervodigital.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "acervo_history", schema = "sc-acervo-digital")
public class AcervoHistoryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "item_public_key", nullable = false)
    private String itemPublicKey;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column(name = "event_code", nullable = false)
    private String eventCode;

    @Column(name = "status_before")
    private String statusBefore;

    @Column(name = "status_after")
    private String statusAfter;

    @Column(name = "reason_code")
    private String reasonCode;

    @Column(name = "reason_description")
    private String reasonDescription;

    @Column(name = "actor_type")
    private String actorType;

    @Column(name = "actor_id")
    private String actorId;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getItemPublicKey() { return itemPublicKey; }
    public void setItemPublicKey(String itemPublicKey) { this.itemPublicKey = itemPublicKey; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getEventCode() { return eventCode; }
    public void setEventCode(String eventCode) { this.eventCode = eventCode; }
    public String getStatusBefore() { return statusBefore; }
    public void setStatusBefore(String statusBefore) { this.statusBefore = statusBefore; }
    public String getStatusAfter() { return statusAfter; }
    public void setStatusAfter(String statusAfter) { this.statusAfter = statusAfter; }
    public String getReasonCode() { return reasonCode; }
    public void setReasonCode(String reasonCode) { this.reasonCode = reasonCode; }
    public String getReasonDescription() { return reasonDescription; }
    public void setReasonDescription(String reasonDescription) { this.reasonDescription = reasonDescription; }
    public String getActorType() { return actorType; }
    public void setActorType(String actorType) { this.actorType = actorType; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getCorrelationId() { return correlationId; }
    public void setCorrelationId(String correlationId) { this.correlationId = correlationId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
