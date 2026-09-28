package br.com.viafluvial.acervodigital.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "media_item")
public class MediaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "media_public_key", nullable = false, unique = true)
    private String mediaPublicKey;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "owner_public_key")
    private String ownerPublicKey;

    @Column(name = "boat_public_key")
    private String boatPublicKey;

    @Column(name = "accommodation_code")
    private String accommodationCode;

    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @Column(name = "purpose", nullable = false)
    private String purpose;

    @Column(name = "visibility", nullable = false)
    private String visibility;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "is_main")
    private Boolean isMain;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "rejection_reason_code")
    private String rejectionReasonCode;

    @Column(name = "rejection_reason_description")
    private String rejectionReasonDescription;

    @Column(name = "object_path", nullable = false)
    private String objectPath;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    @Column(name = "cdn_url")
    private String cdnUrl;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "responsible_declaration")
    private Boolean responsibleDeclaration;

    @Column(name = "has_image_use_authorization")
    private Boolean hasImageUseAuthorization;

    @Column(name = "allows_commercial_use")
    private Boolean allowsCommercialUse;

    @Column(name = "allows_marketing_use")
    private Boolean allowsMarketingUse;

    @Column(name = "allows_social_media_use")
    private Boolean allowsSocialMediaUse;

    @Column(name = "platform_showcase_enabled")
    private Boolean platformShowcaseEnabled;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getMediaPublicKey() { return mediaPublicKey; }
    public void setMediaPublicKey(String mediaPublicKey) { this.mediaPublicKey = mediaPublicKey; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public String getOwnerPublicKey() { return ownerPublicKey; }
    public void setOwnerPublicKey(String ownerPublicKey) { this.ownerPublicKey = ownerPublicKey; }
    public String getBoatPublicKey() { return boatPublicKey; }
    public void setBoatPublicKey(String boatPublicKey) { this.boatPublicKey = boatPublicKey; }
    public String getAccommodationCode() { return accommodationCode; }
    public void setAccommodationCode(String accommodationCode) { this.accommodationCode = accommodationCode; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getMain() { return isMain; }
    public void setMain(Boolean main) { isMain = main; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
    public String getRejectionReasonCode() { return rejectionReasonCode; }
    public void setRejectionReasonCode(String rejectionReasonCode) { this.rejectionReasonCode = rejectionReasonCode; }
    public String getRejectionReasonDescription() { return rejectionReasonDescription; }
    public void setRejectionReasonDescription(String rejectionReasonDescription) { this.rejectionReasonDescription = rejectionReasonDescription; }
    public String getObjectPath() { return objectPath; }
    public void setObjectPath(String objectPath) { this.objectPath = objectPath; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public String getCdnUrl() { return cdnUrl; }
    public void setCdnUrl(String cdnUrl) { this.cdnUrl = cdnUrl; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Boolean getResponsibleDeclaration() { return responsibleDeclaration; }
    public void setResponsibleDeclaration(Boolean responsibleDeclaration) { this.responsibleDeclaration = responsibleDeclaration; }
    public Boolean getHasImageUseAuthorization() { return hasImageUseAuthorization; }
    public void setHasImageUseAuthorization(Boolean hasImageUseAuthorization) { this.hasImageUseAuthorization = hasImageUseAuthorization; }
    public Boolean getAllowsCommercialUse() { return allowsCommercialUse; }
    public void setAllowsCommercialUse(Boolean allowsCommercialUse) { this.allowsCommercialUse = allowsCommercialUse; }
    public Boolean getAllowsMarketingUse() { return allowsMarketingUse; }
    public void setAllowsMarketingUse(Boolean allowsMarketingUse) { this.allowsMarketingUse = allowsMarketingUse; }
    public Boolean getAllowsSocialMediaUse() { return allowsSocialMediaUse; }
    public void setAllowsSocialMediaUse(Boolean allowsSocialMediaUse) { this.allowsSocialMediaUse = allowsSocialMediaUse; }
    public Boolean getPlatformShowcaseEnabled() { return platformShowcaseEnabled; }
    public void setPlatformShowcaseEnabled(Boolean platformShowcaseEnabled) { this.platformShowcaseEnabled = platformShowcaseEnabled; }
    public OffsetDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(OffsetDateTime approvedAt) { this.approvedAt = approvedAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
