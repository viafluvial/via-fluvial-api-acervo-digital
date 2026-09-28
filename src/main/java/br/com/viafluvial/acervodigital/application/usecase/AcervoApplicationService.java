package br.com.viafluvial.acervodigital.application.usecase;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.*;
import br.com.viafluvial.acervodigital.adapters.out.integration.VesselOwnershipClient;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaFileVariantEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.AcervoHistoryEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.PrivateDocumentEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.AcervoHistoryJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaFileVariantJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.PrivateDocumentJpaRepository;
import br.com.viafluvial.acervodigital.application.media.ImageDerivative;
import br.com.viafluvial.acervodigital.application.media.ImageDerivativeService;
import br.com.viafluvial.acervodigital.application.media.WatermarkService;
import br.com.viafluvial.acervodigital.application.ports.ObjectStoragePort;
import br.com.viafluvial.acervodigital.common.id.CurrentActorProvider;
import br.com.viafluvial.acervodigital.common.utils.AssetNamingUtils;
import br.com.viafluvial.acervodigital.common.utils.FileValidationUtils;
import br.com.viafluvial.acervodigital.config.AcervoProperties;
import br.com.viafluvial.acervodigital.config.AcervoUploadProperties;
import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.DocumentTypeCatalogJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.RejectionReasonCatalogJpaRepository;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@Transactional
public class AcervoApplicationService {

    private final MediaJpaRepository mediaRepository;
    private final MediaFileVariantJpaRepository mediaFileVariantRepository;
    private final PrivateDocumentJpaRepository documentRepository;
    private final AcervoHistoryJpaRepository historyRepository;
    private final DocumentTypeCatalogJpaRepository documentTypeCatalogRepository;
    private final RejectionReasonCatalogJpaRepository rejectionReasonCatalogRepository;
    private final AcervoUploadProperties uploadProperties;
    private final AcervoProperties acervoProperties;
    private final ObjectStoragePort objectStoragePort;
    private final ImageDerivativeService imageDerivativeService;
    private final WatermarkService watermarkService;
    private final CurrentActorProvider currentActorProvider;
    private final VesselOwnershipClient vesselOwnershipClient;

    @Value("${acervo.storage.cdn-base-url:https://cdn.viafluvial.com.br}")
    private String cdnBaseUrl;

    @Value("${acervo.security.signed-url-secret:}")
    private String signedUrlSecret;

    public AcervoApplicationService(
        MediaJpaRepository mediaRepository,
        MediaFileVariantJpaRepository mediaFileVariantRepository,
        PrivateDocumentJpaRepository documentRepository,
        AcervoHistoryJpaRepository historyRepository,
        DocumentTypeCatalogJpaRepository documentTypeCatalogRepository,
        RejectionReasonCatalogJpaRepository rejectionReasonCatalogRepository,
        AcervoUploadProperties uploadProperties,
        AcervoProperties acervoProperties,
        ObjectStoragePort objectStoragePort,
        ImageDerivativeService imageDerivativeService,
        WatermarkService watermarkService,
        CurrentActorProvider currentActorProvider,
        VesselOwnershipClient vesselOwnershipClient
    ) {
        this.mediaRepository = mediaRepository;
        this.mediaFileVariantRepository = mediaFileVariantRepository;
        this.documentRepository = documentRepository;
        this.historyRepository = historyRepository;
        this.documentTypeCatalogRepository = documentTypeCatalogRepository;
        this.rejectionReasonCatalogRepository = rejectionReasonCatalogRepository;
        this.uploadProperties = uploadProperties;
        this.acervoProperties = acervoProperties;
        this.objectStoragePort = objectStoragePort;
        this.imageDerivativeService = imageDerivativeService;
        this.watermarkService = watermarkService;
        this.currentActorProvider = currentActorProvider;
        this.vesselOwnershipClient = vesselOwnershipClient;
    }

    public MediaItemResponse uploadMedia(MultipartFile file, EntityType entityType, UUID entityId, AcervoMediaType mediaType,
                                         AcervoPurpose purpose, AcervoVisibility visibility, ActorReference uploadedBy,
                                         String ownerPublicKey, String boatPublicKey, String accommodationCode,
                                         String title, String description, RightsDeclarationRequest rightsDeclaration) {
        try {
            Optional<UUID> scopedActorId = resolveScopedActorId();
            scopedActorId.ifPresent(actorId -> ensureEntityOwnedByScopedActor(entityType, entityId, actorId));

            validateMediaFile(file, mediaType);
            byte[] originalBytes = toBytes(file);

            String effectiveOwnerPublicKey = ownerPublicKey;
            String effectiveBoatPublicKey = boatPublicKey;
            if (scopedActorId.isPresent()) {
                effectiveOwnerPublicKey = scopedActorId.get().toString();
                if (EntityType.EMBARCACAO.equals(entityType)) {
                    effectiveBoatPublicKey = entityId == null ? null : entityId.toString();
                }
            }

            MediaEntity entity = new MediaEntity();
            entity.setId(UUID.randomUUID());
            entity.setMediaPublicKey("med_" + UUID.randomUUID().toString().replace("-", ""));
            entity.setEntityType(entityType.getValue());
            entity.setEntityId(entityId);
            entity.setMediaType(mediaType.getValue());
            entity.setPurpose(purpose.getValue());
            entity.setVisibility(visibility.getValue());
            entity.setStatus(AcervoStatus.AGUARDANDO_APROVACAO.getValue());
            entity.setOwnerPublicKey(effectiveOwnerPublicKey);
            entity.setBoatPublicKey(effectiveBoatPublicKey);
            entity.setAccommodationCode(accommodationCode);
            entity.setDescription(description);
            entity.setMain(Boolean.FALSE);
            entity.setDisplayOrder(0);
            entity.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
            entity.setSizeBytes(file.getSize());
            entity.setSourceType(rightsDeclaration == null || rightsDeclaration.getSourceType() == null
                ? MediaSourceType.TERCEIRO.getValue() : rightsDeclaration.getSourceType().getValue());
            entity.setResponsibleDeclaration(rightsDeclaration != null && rightsDeclaration.getResponsibleDeclaration() != null
                ? rightsDeclaration.getResponsibleDeclaration() : Boolean.FALSE);
            entity.setHasImageUseAuthorization(rightsDeclaration != null && rightsDeclaration.getHasImageUseAuthorization() != null
                ? rightsDeclaration.getHasImageUseAuthorization() : Boolean.FALSE);
            entity.setAllowsCommercialUse(rightsDeclaration != null && rightsDeclaration.getAllowsCommercialUse() != null
                ? rightsDeclaration.getAllowsCommercialUse() : Boolean.FALSE);
            entity.setAllowsMarketingUse(rightsDeclaration != null && rightsDeclaration.getAllowsMarketingUse() != null
                ? rightsDeclaration.getAllowsMarketingUse() : Boolean.FALSE);
            entity.setAllowsSocialMediaUse(rightsDeclaration != null && rightsDeclaration.getAllowsSocialMediaUse() != null
                ? rightsDeclaration.getAllowsSocialMediaUse() : Boolean.FALSE);
            entity.setPlatformShowcaseEnabled(Boolean.FALSE);
            entity.setCreatedAt(OffsetDateTime.now());
            entity.setUpdatedAt(OffsetDateTime.now());
            String relatedEntityName = extractRelatedEntityNameHint(entity.getEntityType(), title);
            entity.setTitle(AssetNamingUtils.buildMediaTitle(
                entity.getMediaType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getMediaPublicKey(),
                entity.getCreatedAt(),
                relatedEntityName
            ));

            String extension = extensionFromMimeType(entity.getMimeType());
            String originalObjectPath = mediaObjectPath(entity, AcervoFileResponse.VariantEnum.ORIGINAL, extension);
            entity.setObjectPath(originalObjectPath);

            MediaEntity saved = mediaRepository.save(entity);
            persistMediaVariants(saved, originalBytes, extension);
            recordHistory(saved.getMediaPublicKey(), "MEDIA", "ACERVO_UPLOAD_REALIZADO", null, saved.getStatus(), null, null, uploadedBy, null);
            return toMediaResponse(saved);
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            String reason = ex.getMessage() == null || ex.getMessage().isBlank() ? ex.getClass().getSimpleName() : ex.getMessage();
            throw new DomainException("MEDIA_UPLOAD_ERROR", "Falha ao processar upload da mídia: " + reason, 500);
        }
    }

    public MediaItemResponse getMedia(String mediaPublicKey) {
        return toMediaResponse(requireAccessibleMedia(mediaPublicKey));
    }

    public PagedMediaResponse searchMedia(EntityType entityType, UUID entityId, AcervoMediaType mediaType, AcervoPurpose purpose,
                                          AcervoVisibility visibility, AcervoStatus status, Integer page, Integer size) {
        Optional<UUID> scopedActorId = resolveScopedActorId();
        scopedActorId.ifPresent(actorId -> {
            if (entityType != null && entityId != null) {
                ensureEntityOwnedByScopedActor(entityType, entityId, actorId);
            }
        });

        Map<UUID, Boolean> vesselOwnershipCache = new HashMap<>();
        Stream<MediaEntity> stream = mediaRepository.findAll().stream();
        if (entityType != null) stream = stream.filter(m -> m.getEntityType().equals(entityType.getValue()));
        if (entityId != null) stream = stream.filter(m -> m.getEntityId().equals(entityId));
        if (mediaType != null) stream = stream.filter(m -> m.getMediaType().equals(mediaType.getValue()));
        if (purpose != null) stream = stream.filter(m -> m.getPurpose().equals(purpose.getValue()));
        if (visibility != null) stream = stream.filter(m -> m.getVisibility().equals(visibility.getValue()));
        if (status != null) stream = stream.filter(m -> m.getStatus().equals(status.getValue()));
        if (scopedActorId.isPresent()) {
            UUID actorId = scopedActorId.get();
            stream = stream.filter(m -> isEntityOwnedByScopedActor(m.getEntityType(), m.getEntityId(), actorId, vesselOwnershipCache));
        }
        List<MediaEntity> filtered = stream.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
        return toPagedMedia(filtered, page, size);
    }

    public PagedMediaResponse listMediaByEntity(EntityType entityType, UUID entityId, Boolean onlyPublished, Integer page, Integer size) {
        resolveScopedActorId().ifPresent(actorId -> ensureEntityOwnedByScopedActor(entityType, entityId, actorId));

        List<MediaEntity> filtered = mediaRepository.findAll().stream()
            .filter(m -> m.getEntityType().equals(entityType.getValue()) && m.getEntityId().equals(entityId))
            .filter(m -> !Boolean.TRUE.equals(onlyPublished) || AcervoStatus.PUBLICADO.getValue().equals(m.getStatus()))
            .sorted((a, b) -> Integer.compare(a.getDisplayOrder() == null ? Integer.MAX_VALUE : a.getDisplayOrder(),
                b.getDisplayOrder() == null ? Integer.MAX_VALUE : b.getDisplayOrder()))
            .toList();
        return toPagedMedia(filtered, page, size);
    }

    public List<MediaItemResponse> listPublicHeroMedia(Integer limit) {
        int safeLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 30));

        return mediaRepository.findAll().stream()
            .filter(this::isEligibleForPublicHero)
            .sorted((left, right) -> resolveHeroSortDate(right).compareTo(resolveHeroSortDate(left)))
            .limit(safeLimit)
            .map(this::toMediaResponse)
            .toList();
    }

    public boolean isMediaShowcaseEnabled(String mediaPublicKey) {
        MediaEntity media = requireMedia(mediaPublicKey);
        ensurePhotoOnlyForShowcase(media);
        return Boolean.TRUE.equals(media.getPlatformShowcaseEnabled());
    }

    public boolean updateMediaShowcaseEnabled(String mediaPublicKey, boolean enabled, ActorReference changedBy) {
        MediaEntity media = requireMedia(mediaPublicKey);
        ensurePhotoOnlyForShowcase(media);

        media.setPlatformShowcaseEnabled(enabled);
        media.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(media);

        recordHistory(
            mediaPublicKey,
            "MEDIA",
            "ACERVO_MIDIA_VITRINE_PLATAFORMA_ATUALIZADA",
            media.getStatus(),
            media.getStatus(),
            null,
            "platformShowcaseEnabled=" + enabled,
            changedBy,
            null
        );

        return Boolean.TRUE.equals(media.getPlatformShowcaseEnabled());
    }

    public DecisionResponse approveMedia(String mediaPublicKey, ApprovalDecisionRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        String before = entity.getStatus();
        entity.setStatus(AcervoStatus.APROVADO.getValue());
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(entity);
        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_APROVADA", before, entity.getStatus(), null, request.getObservation(), request.getDecidedBy(), null);
        return decision("MEDIA_APPROVED", "Mídia aprovada com sucesso.", mediaPublicKey, entity.getStatus());
    }

    public DecisionResponse rejectMedia(String mediaPublicKey, RejectionDecisionRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        String before = entity.getStatus();
        entity.setStatus(AcervoStatus.REPROVADO.getValue());
        entity.setRejectionReasonCode(request.getReasonCode());
        entity.setRejectionReasonDescription(request.getObservation());
        entity.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(entity);
        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_REPROVADA", before, entity.getStatus(), request.getReasonCode(), request.getObservation(), request.getDecidedBy(), null);
        return decision("MEDIA_REJECTED", "Mídia reprovada com sucesso.", mediaPublicKey, entity.getStatus());
    }

    public MediaItemResponse publishMedia(String mediaPublicKey, PublishMediaRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        if (!AcervoStatus.APROVADO.getValue().equals(entity.getStatus())) {
            throw new DomainException("MEDIA_NOT_APPROVED", "A mídia precisa estar aprovada para publicação.", 409);
        }
        if (!AcervoVisibility.PUBLICA.getValue().equals(entity.getVisibility())) {
            throw new DomainException("MEDIA_NOT_PUBLIC", "A mídia precisa possuir visibilidade pública para publicação.", 422);
        }

        List<MediaFileVariantEntity> variants = mediaFileVariantRepository.findByMediaIdOrderByCreatedAtAsc(entity.getId());
        if (variants.isEmpty()) {
            throw new DomainException("MEDIA_VARIANTS_NOT_FOUND", "Nenhuma variante de arquivo foi encontrada para a mídia.", 409);
        }

        for (MediaFileVariantEntity variant : variants) {
            String targetObjectPath = variant.getObjectPath().replaceFirst("^quarantine/", "public-media/");
            String sourceBucketName = resolveBucketNameByType(variant.getBucketType());
            if (shouldApplyWatermark(entity, variant)) {
                byte[] sourceContent = objectStoragePort.getObject(
                    sourceBucketName,
                    variant.getObjectPath()
                );
                byte[] watermarked = watermarkService.applyBottomRightWatermark(sourceContent, variant.getMimeType());
                objectStoragePort.putObject(
                    acervoProperties.storage().publicMediaBucket(),
                    targetObjectPath,
                    watermarked,
                    variant.getMimeType()
                );
                variant.setSizeBytes((long) watermarked.length);
                variant.setChecksum(sha256(watermarked));
            } else {
                objectStoragePort.copyObject(
                    sourceBucketName,
                    variant.getObjectPath(),
                    acervoProperties.storage().publicMediaBucket(),
                    targetObjectPath
                );
            }
            variant.setBucketType(AcervoFileResponse.BucketTypeEnum.PUBLIC_MEDIA.getValue());
            variant.setObjectPath(targetObjectPath);
            variant.setUpdatedAt(OffsetDateTime.now());
        }
        mediaFileVariantRepository.saveAll(variants);

        String before = entity.getStatus();
        entity.setStatus(AcervoStatus.PUBLICADO.getValue());
        entity.setPublishedAt(OffsetDateTime.now());
        if (request == null || request.getMakeAvailableOnCdn() == null || request.getMakeAvailableOnCdn()) {
            entity.setCdnUrl(cdnBaseUrl + "/" + entity.getMediaPublicKey());
        }
        entity.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(entity);
        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_PUBLICADA", before, entity.getStatus(), null,
            request == null ? null : request.getObservation(), request == null ? null : request.getPublishedBy(), null);
        return toMediaResponse(entity);
    }

    public DecisionResponse blockMedia(String mediaPublicKey, BlockMediaRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        String before = entity.getStatus();
        entity.setStatus(AcervoStatus.BLOQUEADO.getValue());
        entity.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(entity);
        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_BLOQUEADA", before, entity.getStatus(),
            request.getReasonCode(), request.getObservation(), request.getBlockedBy(), null);
        return decision("MEDIA_BLOCKED", "Mídia bloqueada com sucesso.", mediaPublicKey, entity.getStatus());
    }

    public DecisionResponse archiveMedia(String mediaPublicKey, ArchiveRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        String before = entity.getStatus();
        entity.setStatus(AcervoStatus.ARQUIVADO.getValue());
        entity.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(entity);
        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_ARQUIVADA", before, entity.getStatus(), null,
            request == null ? null : request.getReason(), request == null ? null : request.getArchivedBy(), null);
        return decision("MEDIA_ARCHIVED", "Mídia arquivada com sucesso.", mediaPublicKey, entity.getStatus());
    }

    public MediaItemResponse setMainMedia(String mediaPublicKey, SetMainMediaRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        if (!AcervoStatus.APROVADO.getValue().equals(entity.getStatus()) && !AcervoStatus.PUBLICADO.getValue().equals(entity.getStatus())) {
            throw new DomainException("INVALID_MAIN_MEDIA", "A mídia precisa estar aprovada ou publicada para ser principal.", 409);
        }

        mediaRepository.findAll().stream()
            .filter(m -> m.getEntityType().equals(entity.getEntityType()) && m.getEntityId().equals(entity.getEntityId()))
            .forEach(m -> {
                m.setMain(m.getMediaPublicKey().equals(entity.getMediaPublicKey()) && request.getMain());
                m.setUpdatedAt(OffsetDateTime.now());
                mediaRepository.save(m);
            });

        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_PRINCIPAL_ATUALIZADA", entity.getStatus(), entity.getStatus(), null,
            "main=" + request.getMain(), request.getChangedBy(), null);
        return toMediaResponse(requireAccessibleMedia(mediaPublicKey));
    }

    public MediaItemResponse updateMediaOrder(String mediaPublicKey, UpdateOrderRequest request) {
        MediaEntity entity = requireAccessibleMedia(mediaPublicKey);
        entity.setDisplayOrder(request.getDisplayOrder());
        entity.setUpdatedAt(OffsetDateTime.now());
        mediaRepository.save(entity);
        recordHistory(mediaPublicKey, "MEDIA", "ACERVO_MIDIA_ORDEM_ATUALIZADA", entity.getStatus(), entity.getStatus(), null,
            "displayOrder=" + request.getDisplayOrder(), request.getChangedBy(), null);
        return toMediaResponse(entity);
    }

    public PrivateDocumentResponse uploadPrivateDocument(MultipartFile file, EntityType entityType, UUID entityId, String documentTypeCode,
                                                         ActorReference uploadedBy, String documentNumber, String issuer,
                                                         LocalDate issuedAt, LocalDate expiresAt) {
        resolveScopedActorId().ifPresent(actorId -> ensureEntityOwnedByScopedActor(entityType, entityId, actorId));

        validateDocumentFile(file);

        PrivateDocumentEntity entity = new PrivateDocumentEntity();
        entity.setId(UUID.randomUUID());
        entity.setDocumentPublicKey("doc_" + UUID.randomUUID().toString().replace("-", ""));
        entity.setEntityType(entityType.getValue());
        entity.setEntityId(entityId);
        entity.setDocumentTypeCode(documentTypeCode);
        entity.setDocumentTypeName(documentTypeCode.replace('_', ' '));
        entity.setDocumentNumber(documentNumber);
        entity.setIssuer(issuer);
        entity.setIssuedAt(issuedAt);
        entity.setExpiresAt(expiresAt);
        entity.setStatus(DocumentStatus.AGUARDANDO_APROVACAO.getValue());
        entity.setVisibility(AcervoVisibility.PRIVADA.getValue());
        entity.setMimeType(file.getContentType() == null ? "application/octet-stream" : file.getContentType());
        entity.setSizeBytes(file.getSize());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        String extension = extensionFromMimeType(entity.getMimeType());
        String standardizedName = AssetNamingUtils.buildPrivateDocumentFileName(
            entity.getEntityType(),
            entity.getEntityId(),
            entity.getDocumentTypeCode(),
            entity.getDocumentPublicKey(),
            entity.getCreatedAt(),
            extension
        );
        entity.setObjectPath(AssetNamingUtils.buildPrivateDocumentObjectPath(entity.getDocumentPublicKey(), standardizedName));

        objectStoragePort.putObject(
            acervoProperties.storage().privateDocumentsBucket(),
            entity.getObjectPath(),
            toBytes(file),
            entity.getMimeType()
        );

        documentRepository.save(entity);
        recordHistory(entity.getDocumentPublicKey(), "DOCUMENT", "ACERVO_DOCUMENTO_ENVIADO", null, entity.getStatus(), null, null, uploadedBy, null);
        return toDocumentResponse(entity);
    }

    public PrivateDocumentResponse getPrivateDocument(String documentPublicKey) {
        return toDocumentResponse(requireAccessibleDocument(documentPublicKey));
    }

    public BinaryContent getMediaContent(String mediaPublicKey, String requestedVariant) {
        MediaEntity media = requireAccessibleMedia(mediaPublicKey);
        List<MediaFileVariantEntity> variants = mediaFileVariantRepository.findByMediaIdOrderByCreatedAtAsc(media.getId());

        if (variants.isEmpty()) {
            throw new DomainException("MEDIA_VARIANTS_NOT_FOUND", "Nenhuma variante de arquivo foi encontrada para a mídia.", 404);
        }

        MediaFileVariantEntity variant = pickMediaVariant(variants, requestedVariant);
        String bucketName = resolveBucketNameByType(variant.getBucketType());
        byte[] content = objectStoragePort.getObject(bucketName, variant.getObjectPath());
        return new BinaryContent(content, variant.getMimeType(), extractFilename(variant.getObjectPath()));
    }

    public BinaryContent getPrivateDocumentContent(String documentPublicKey) {
        PrivateDocumentEntity document = requireAccessibleDocument(documentPublicKey);
        byte[] content = objectStoragePort.getObject(
            acervoProperties.storage().privateDocumentsBucket(),
            document.getObjectPath()
        );
        return new BinaryContent(content, document.getMimeType(), extractFilename(document.getObjectPath()));
    }

    public PagedPrivateDocumentResponse searchPrivateDocuments(EntityType entityType, UUID entityId, String documentTypeCode,
                                                               DocumentStatus status, LocalDate expiresUntil, Integer page, Integer size) {
        Optional<UUID> scopedActorId = resolveScopedActorId();
        scopedActorId.ifPresent(actorId -> {
            if (entityType != null && entityId != null) {
                ensureEntityOwnedByScopedActor(entityType, entityId, actorId);
            }
        });

        Map<UUID, Boolean> vesselOwnershipCache = new HashMap<>();
        Stream<PrivateDocumentEntity> stream = documentRepository.findAll().stream();
        if (entityType != null) stream = stream.filter(d -> d.getEntityType().equals(entityType.getValue()));
        if (entityId != null) stream = stream.filter(d -> d.getEntityId().equals(entityId));
        if (documentTypeCode != null) stream = stream.filter(d -> d.getDocumentTypeCode().equals(documentTypeCode));
        if (status != null) stream = stream.filter(d -> d.getStatus().equals(status.getValue()));
        if (expiresUntil != null) stream = stream.filter(d -> d.getExpiresAt() != null && !d.getExpiresAt().isAfter(expiresUntil));
        if (scopedActorId.isPresent()) {
            UUID actorId = scopedActorId.get();
            stream = stream.filter(d -> isEntityOwnedByScopedActor(d.getEntityType(), d.getEntityId(), actorId, vesselOwnershipCache));
        }

        List<PrivateDocumentEntity> filtered = stream.sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())).toList();
        int safePage = page == null ? 0 : page;
        int safeSize = size == null || size < 1 ? 20 : size;
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());

        List<PrivateDocumentResponse> content = filtered.subList(fromIndex, toIndex).stream().map(this::toDocumentResponse).toList();
        PageMetadata metadata = new PageMetadata(safePage, safeSize, (long) filtered.size(), (int) Math.ceil((double) filtered.size() / safeSize));
        return new PagedPrivateDocumentResponse(content, metadata);
    }

    public SignedUrlResponse createSignedUrl(String documentPublicKey, SignedUrlRequest request, String correlationId) {
        PrivateDocumentEntity document = requireAccessibleDocument(documentPublicKey);
        int expiresIn = request != null && request.getExpiresInSeconds() != null ? request.getExpiresInSeconds() : 300;
        OffsetDateTime expiresAt = OffsetDateTime.now().plusSeconds(expiresIn);
        String purpose = request == null || request.getPurpose() == null ? "read" : request.getPurpose();

        if (signedUrlSecret == null || signedUrlSecret.isBlank()) {
            throw new DomainException("SIGNED_URL_SECRET_MISSING", "Configuração acervo.security.signed-url-secret é obrigatória para gerar URL assinada.", 500);
        }

        String payload = documentPublicKey + ":" + expiresAt.toEpochSecond() + ":" + purpose;
        String signature = hmacSha256Base64Url(payload, signedUrlSecret);
        String purposeEncoded = URLEncoder.encode(purpose, StandardCharsets.UTF_8);
        URI signedUri = URI.create(cdnBaseUrl + "/signed/private/" + documentPublicKey
            + "?expiresAt=" + expiresAt.toEpochSecond()
            + "&purpose=" + purposeEncoded
            + "&sig=" + signature);

        recordHistory(documentPublicKey, "DOCUMENT", "ACERVO_DOCUMENTO_URL_ASSINADA_GERADA", document.getStatus(), document.getStatus(), null,
            request == null ? null : request.getPurpose(), request == null ? null : request.getRequestedBy(), correlationId);

        return new SignedUrlResponse(signedUri, expiresAt, correlationId);
    }

    public DecisionResponse approvePrivateDocument(String documentPublicKey, ApprovalDecisionRequest request) {
        PrivateDocumentEntity entity = requireAccessibleDocument(documentPublicKey);
        String before = entity.getStatus();
        entity.setStatus(DocumentStatus.APROVADO.getValue());
        entity.setApprovedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(entity);
        recordHistory(documentPublicKey, "DOCUMENT", "ACERVO_DOCUMENTO_APROVADO", before, entity.getStatus(), null, request.getObservation(), request.getDecidedBy(), null);
        return decision("PRIVATE_DOCUMENT_APPROVED", "Documento aprovado com sucesso.", documentPublicKey, entity.getStatus());
    }

    public DecisionResponse rejectPrivateDocument(String documentPublicKey, RejectionDecisionRequest request) {
        PrivateDocumentEntity entity = requireAccessibleDocument(documentPublicKey);
        String before = entity.getStatus();
        entity.setStatus(DocumentStatus.REPROVADO.getValue());
        entity.setRejectionReasonCode(request.getReasonCode());
        entity.setRejectionReasonDescription(request.getObservation());
        entity.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(entity);
        recordHistory(documentPublicKey, "DOCUMENT", "ACERVO_DOCUMENTO_REPROVADO", before, entity.getStatus(), request.getReasonCode(), request.getObservation(), request.getDecidedBy(), null);
        return decision("PRIVATE_DOCUMENT_REJECTED", "Documento reprovado com sucesso.", documentPublicKey, entity.getStatus());
    }

    public DecisionResponse removePrivateDocument(String documentPublicKey) {
        PrivateDocumentEntity entity = requireAccessibleDocument(documentPublicKey);
        String before = entity.getStatus();

        objectStoragePort.deleteObject(
            acervoProperties.storage().privateDocumentsBucket(),
            entity.getObjectPath()
        );

        entity.setStatus(DocumentStatus.REMOVIDO.getValue());
        entity.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(entity);

        ActorReference actor = new ActorReference();
        actor.setActorType(ActorReference.ActorTypeEnum.SERVICE);
        actor.setActorId("acervo-service");

        recordHistory(documentPublicKey, "DOCUMENT", "ACERVO_DOCUMENTO_REMOVIDO", before, entity.getStatus(), null, "Documento removido do acervo.", actor, null);
        return decision("PRIVATE_DOCUMENT_REMOVED", "Documento removido com sucesso.", documentPublicKey, entity.getStatus());
    }

    public List<DocumentTypeResponse> listDocumentTypes(DocumentScope scope, Boolean active) {
        Stream<br.com.viafluvial.acervodigital.adapters.out.persistence.entity.DocumentTypeCatalogEntity> stream;

        if (scope != null && active != null) {
            stream = documentTypeCatalogRepository.findByScopeAndActive(scope.getValue(), active).stream();
        } else if (scope != null) {
            stream = documentTypeCatalogRepository.findByScope(scope.getValue()).stream();
        } else if (active != null) {
            stream = documentTypeCatalogRepository.findByActive(active).stream();
        } else {
            stream = documentTypeCatalogRepository.findAll().stream();
        }

        return stream
            .map(e -> new DocumentTypeResponse(e.getCode(), e.getName(), DocumentScope.fromValue(e.getScope()),
                e.getActive(), e.getRequiresExpiration(), e.getAllowsCommercialUse(), e.getAllowsSocialMediaUse()))
            .toList();
    }

    public List<RejectionReasonResponse> listRejectionReasons(String targetType) {
        Stream<br.com.viafluvial.acervodigital.adapters.out.persistence.entity.RejectionReasonCatalogEntity> stream;

        if (targetType != null) {
            stream = rejectionReasonCatalogRepository.findByTargetTypeAndActive(targetType, true).stream();
        } else {
            stream = rejectionReasonCatalogRepository.findByActive(true).stream();
        }

        return stream
            .map(e -> new RejectionReasonResponse(e.getCode(), e.getDescription(),
                RejectionReasonResponse.TargetTypeEnum.fromValue(e.getTargetType()), e.getActive()))
            .toList();
    }

    public PagedApprovalQueueResponse approvalQueue(String itemType, EntityType entityType, UUID entityId, Integer page, Integer size) {
        Optional<UUID> scopedActorId = resolveScopedActorId();
        scopedActorId.ifPresent(actorId -> {
            if (entityType != null && entityId != null) {
                ensureEntityOwnedByScopedActor(entityType, entityId, actorId);
            }
        });

        Map<UUID, Boolean> vesselOwnershipCache = new HashMap<>();
        List<ApprovalQueueItem> items = new ArrayList<>();

        mediaRepository.findAll().stream()
            .filter(m -> AcervoStatus.AGUARDANDO_APROVACAO.getValue().equals(m.getStatus()))
            .filter(m -> entityType == null || m.getEntityType().equals(entityType.getValue()))
            .filter(m -> entityId == null || m.getEntityId().equals(entityId))
            .filter(m -> scopedActorId.isEmpty() || isEntityOwnedByScopedActor(m.getEntityType(), m.getEntityId(), scopedActorId.get(), vesselOwnershipCache))
            .filter(m -> itemType == null || "MEDIA".equalsIgnoreCase(itemType))
            .forEach(m -> items.add(new ApprovalQueueItem(m.getMediaPublicKey(), ApprovalQueueItem.ItemTypeEnum.MEDIA,
                EntityType.fromValue(m.getEntityType()), m.getEntityId(), m.getStatus(), m.getCreatedAt()).title(m.getTitle())));

        documentRepository.findAll().stream()
            .filter(d -> DocumentStatus.AGUARDANDO_APROVACAO.getValue().equals(d.getStatus()))
            .filter(d -> entityType == null || d.getEntityType().equals(entityType.getValue()))
            .filter(d -> entityId == null || d.getEntityId().equals(entityId))
            .filter(d -> scopedActorId.isEmpty() || isEntityOwnedByScopedActor(d.getEntityType(), d.getEntityId(), scopedActorId.get(), vesselOwnershipCache))
            .filter(d -> itemType == null || "DOCUMENT".equalsIgnoreCase(itemType))
            .forEach(d -> items.add(new ApprovalQueueItem(d.getDocumentPublicKey(), ApprovalQueueItem.ItemTypeEnum.DOCUMENT,
                EntityType.fromValue(d.getEntityType()), d.getEntityId(), d.getStatus(), d.getCreatedAt()).title(d.getDocumentTypeName())));

        items.sort((a, b) -> a.getSubmittedAt().compareTo(b.getSubmittedAt()));
        int safePage = page == null ? 0 : page;
        int safeSize = size == null || size < 1 ? 20 : size;
        int fromIndex = Math.min(safePage * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());

        PageMetadata metadata = new PageMetadata(safePage, safeSize, (long) items.size(), (int) Math.ceil((double) items.size() / safeSize));
        return new PagedApprovalQueueResponse(items.subList(fromIndex, toIndex), metadata);
    }

    public List<AcervoHistoryEntry> itemHistory(String itemPublicKey) {
        resolveScopedActorId().ifPresent(actorId -> ensureHistoryItemOwnedByScopedActor(itemPublicKey, actorId));

        return historyRepository.findByItemPublicKeyOrderByCreatedAtDesc(itemPublicKey).stream()
            .map(h -> new AcervoHistoryEntry(h.getEventCode(), h.getStatusAfter(),
                new ActorReference(ActorReference.ActorTypeEnum.fromValue(defaultValue(h.getActorType(), "SERVICE")),
                    defaultValue(h.getActorId(), "system")), h.getCreatedAt())
                .statusBefore(h.getStatusBefore())
                .reasonCode(h.getReasonCode())
                .observation(h.getReasonDescription()))
            .toList();
    }

    public PendingItemsReportResponse pendingItemsReport(String ownerPublicKey, String boatPublicKey) {
        Optional<UUID> scopedActorId = resolveScopedActorId();
        Map<UUID, Boolean> vesselOwnershipCache = new HashMap<>();

        Stream<MediaEntity> mediaStream = mediaRepository.findAll().stream();
        if (scopedActorId.isPresent()) {
            UUID actorId = scopedActorId.get();
            mediaStream = mediaStream.filter(m -> isEntityOwnedByScopedActor(m.getEntityType(), m.getEntityId(), actorId, vesselOwnershipCache));
        } else {
            mediaStream = mediaStream
                .filter(m -> ownerPublicKey == null || ownerPublicKey.equals(m.getOwnerPublicKey()))
                .filter(m -> boatPublicKey == null || boatPublicKey.equals(m.getBoatPublicKey()));
        }

        List<MediaEntity> scopedMedia = mediaStream.toList();
        long pendingMedia = scopedMedia.stream()
            .filter(m -> AcervoStatus.AGUARDANDO_APROVACAO.getValue().equals(m.getStatus()))
            .count();

        Stream<PrivateDocumentEntity> documentStream = documentRepository.findAll().stream();
        if (scopedActorId.isPresent()) {
            UUID actorId = scopedActorId.get();
            documentStream = documentStream.filter(d -> isEntityOwnedByScopedActor(d.getEntityType(), d.getEntityId(), actorId, vesselOwnershipCache));
        }
        List<PrivateDocumentEntity> scopedDocuments = documentStream.toList();

        long pendingDocuments = scopedDocuments.stream()
            .filter(d -> DocumentStatus.AGUARDANDO_APROVACAO.getValue().equals(d.getStatus()))
            .count();

        long expiredDocs = scopedDocuments.stream()
            .filter(d -> d.getExpiresAt() != null && d.getExpiresAt().isBefore(LocalDate.now()))
            .count();

        long blocked = Stream.concat(
            scopedMedia.stream().map(m -> m == null ? null : m.getStatus()),
            scopedDocuments.stream().map(d -> d == null ? null : d.getStatus()))
            .filter(statusValue -> "BLOQUEADO".equals(statusValue))
            .count();

        return new PendingItemsReportResponse((int) pendingMedia, (int) pendingDocuments, (int) expiredDocs, (int) blocked)
            .generatedAt(OffsetDateTime.now());
    }

    private PagedMediaResponse toPagedMedia(List<MediaEntity> filtered, Integer page, Integer size) {
        int safePage = page == null ? 0 : page;
        int safeSize = size == null || size < 1 ? 20 : size;
        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());

        List<MediaItemResponse> content = filtered.subList(fromIndex, toIndex).stream().map(this::toMediaResponse).toList();
        PageMetadata metadata = new PageMetadata(safePage, safeSize, (long) filtered.size(), (int) Math.ceil((double) filtered.size() / safeSize));
        return new PagedMediaResponse(content, metadata);
    }

    private boolean isEligibleForPublicHero(MediaEntity media) {
        if (media == null) {
            return false;
        }

        if (!AcervoMediaType.IMAGE.getValue().equals(media.getMediaType())) {
            return false;
        }

        if (!AcervoVisibility.PUBLICA.getValue().equals(media.getVisibility())) {
            return false;
        }

        if (!AcervoStatus.PUBLICADO.getValue().equals(media.getStatus())) {
            return false;
        }

        if (!Boolean.TRUE.equals(media.getPlatformShowcaseEnabled())) {
            return false;
        }

        if (!MediaSourceType.ENVIADA_PELO_BARQUEIRO.getValue().equals(media.getSourceType())) {
            return false;
        }

        boolean responsibleDeclaration = Boolean.TRUE.equals(media.getResponsibleDeclaration());
        boolean hasImageUseAuthorization = Boolean.TRUE.equals(media.getHasImageUseAuthorization());
        boolean allowsUsage = Boolean.TRUE.equals(media.getAllowsMarketingUse()) || Boolean.TRUE.equals(media.getAllowsCommercialUse());

        return responsibleDeclaration && hasImageUseAuthorization && allowsUsage;
    }

    private void ensurePhotoOnlyForShowcase(MediaEntity media) {
        if (!AcervoMediaType.IMAGE.getValue().equals(media.getMediaType())) {
            throw new DomainException(
                "MEDIA_SHOWCASE_ONLY_PHOTO",
                "Somente fotos podem ser marcadas para vitrine da plataforma.",
                422
            );
        }
    }

    private OffsetDateTime resolveHeroSortDate(MediaEntity media) {
        if (media == null) {
            return OffsetDateTime.MIN;
        }

        if (media.getPublishedAt() != null) {
            return media.getPublishedAt();
        }

        if (media.getUpdatedAt() != null) {
            return media.getUpdatedAt();
        }

        return media.getCreatedAt() == null ? OffsetDateTime.MIN : media.getCreatedAt();
    }

    private MediaEntity requireMedia(String mediaPublicKey) {
        return mediaRepository.findByMediaPublicKey(mediaPublicKey)
            .orElseThrow(() -> new DomainException("MEDIA_NOT_FOUND", "Mídia não encontrada.", 404));
    }

    private MediaEntity requireAccessibleMedia(String mediaPublicKey) {
        MediaEntity media = requireMedia(mediaPublicKey);
        resolveScopedActorId().ifPresent(actorId -> ensureEntityOwnedByScopedActor(media.getEntityType(), media.getEntityId(), actorId));
        return media;
    }

    private String extractRelatedEntityNameHint(String entityType, String titleHint) {
        if (entityType == null || !"EMBARCACAO".equalsIgnoreCase(entityType)) {
            return null;
        }
        if (titleHint == null || titleHint.isBlank()) {
            return null;
        }

        String marker = "EMBARCACAO_NOME:";
        if (!titleHint.startsWith(marker)) {
            return null;
        }

        String value = titleHint.substring(marker.length()).trim();
        return value.isBlank() ? null : value;
    }

    private PrivateDocumentEntity requireDocument(String documentPublicKey) {
        return documentRepository.findByDocumentPublicKey(documentPublicKey)
            .orElseThrow(() -> new DomainException("DOCUMENT_NOT_FOUND", "Documento não encontrado.", 404));
    }

    private PrivateDocumentEntity requireAccessibleDocument(String documentPublicKey) {
        PrivateDocumentEntity document = requireDocument(documentPublicKey);
        resolveScopedActorId().ifPresent(actorId -> ensureEntityOwnedByScopedActor(document.getEntityType(), document.getEntityId(), actorId));
        return document;
    }

    private Optional<UUID> resolveScopedActorId() {
        if (!currentActorProvider.isBoatmanScopedActor()) {
            return Optional.empty();
        }

        return Optional.of(currentActorProvider.currentActorId());
    }

    private void ensureHistoryItemOwnedByScopedActor(String itemPublicKey, UUID actorId) {
        Optional<MediaEntity> media = mediaRepository.findByMediaPublicKey(itemPublicKey);
        if (media.isPresent()) {
            ensureEntityOwnedByScopedActor(media.get().getEntityType(), media.get().getEntityId(), actorId);
            return;
        }

        Optional<PrivateDocumentEntity> document = documentRepository.findByDocumentPublicKey(itemPublicKey);
        if (document.isPresent()) {
            ensureEntityOwnedByScopedActor(document.get().getEntityType(), document.get().getEntityId(), actorId);
            return;
        }

        throw new DomainException("ITEM_NOT_FOUND", "Item de acervo nao encontrado.", 404);
    }

    private void ensureEntityOwnedByScopedActor(EntityType entityType, UUID entityId, UUID actorId) {
        if (!isEntityOwnedByScopedActor(entityType, entityId, actorId, new HashMap<>())) {
            throw new DomainException("ACERVO_SCOPE_FORBIDDEN", "Barqueiro nao pode acessar dados de outro barqueiro.", 403);
        }
    }

    private void ensureEntityOwnedByScopedActor(String entityType, UUID entityId, UUID actorId) {
        if (!isEntityOwnedByScopedActor(entityType, entityId, actorId, new HashMap<>())) {
            throw new DomainException("ACERVO_SCOPE_FORBIDDEN", "Barqueiro nao pode acessar dados de outro barqueiro.", 403);
        }
    }

    private boolean isEntityOwnedByScopedActor(EntityType entityType, UUID entityId, UUID actorId, Map<UUID, Boolean> vesselOwnershipCache) {
        if (entityType == null) {
            return false;
        }

        return isEntityOwnedByScopedActor(entityType.getValue(), entityId, actorId, vesselOwnershipCache);
    }

    private boolean isEntityOwnedByScopedActor(String entityType, UUID entityId, UUID actorId, Map<UUID, Boolean> vesselOwnershipCache) {
        if (entityType == null || entityType.isBlank() || entityId == null || actorId == null) {
            return false;
        }

        String normalizedEntityType = entityType.trim().toUpperCase(Locale.ROOT);
        if ("BARQUEIRO".equals(normalizedEntityType)) {
            return actorId.equals(entityId);
        }

        if ("EMBARCACAO".equals(normalizedEntityType)) {
            return vesselOwnershipCache.computeIfAbsent(entityId, id -> actorId.equals(vesselOwnershipClient.resolveBoatmanId(id)));
        }

        return false;
    }

    private void validateMediaFile(MultipartFile file, AcervoMediaType mediaType) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("INVALID_FILE", "Arquivo é obrigatório.", 400);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new DomainException("INVALID_FILENAME", "Nome de arquivo inválido.", 400);
        }

        if (FileValidationUtils.isPathTraversalAttempt(originalName)) {
            throw new DomainException("INVALID_FILENAME", "Path traversal detected in filename.", 400);
        }

        String mimeType = file.getContentType();
        validateMimeType(mimeType, mediaType);
        validateFileSize(file.getSize(), mediaType);
        validateFileContent(file, mediaType, mimeType);
    }

    private void validateDocumentFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new DomainException("INVALID_FILE", "Arquivo é obrigatório.", 400);
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new DomainException("INVALID_FILENAME", "Nome de arquivo inválido.", 400);
        }

        if (FileValidationUtils.isPathTraversalAttempt(originalName)) {
            throw new DomainException("INVALID_FILENAME", "Path traversal detected in filename.", 400);
        }

        String mimeType = file.getContentType();
        if (mimeType == null || mimeType.isBlank()) {
            throw new DomainException("INVALID_MIMETYPE", "Tipo MIME não pode estar vazio.", 400);
        }

        if (!uploadProperties.getAllowedDocumentMimes().contains(mimeType)) {
            throw new DomainException("UNSUPPORTED_MIMETYPE",
                "Tipo MIME " + mimeType + " não é permitido para documento.", 415);
        }

        long fileSizeBytes = file.getSize();
        if (fileSizeBytes > uploadProperties.getMaxDocumentSizeBytes()) {
            throw new DomainException("FILE_TOO_LARGE",
                "Arquivo excede o tamanho máximo permitido de " + (uploadProperties.getMaxDocumentSizeBytes() / 1024 / 1024) + "MB.", 413);
        }
    }

    private void validateMimeType(String mimeType, AcervoMediaType mediaType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw new DomainException("INVALID_MIMETYPE", "Tipo MIME não pode estar vazio.", 400);
        }

        List<String> allowedMimes = getAllowedMimesForType(mediaType);
        if (!allowedMimes.contains(mimeType)) {
            throw new DomainException("UNSUPPORTED_MIMETYPE",
                "Tipo MIME " + mimeType + " não é permitido para " + mediaType.getValue(), 415);
        }

        if (mediaType == AcervoMediaType.IMAGE && !mimeType.startsWith("image/")) {
            throw new DomainException("UNSUPPORTED_MIMETYPE", "Mídia IMAGE exige tipo MIME de imagem.", 415);
        }

        if (mediaType == AcervoMediaType.VIDEO && !mimeType.startsWith("video/")) {
            throw new DomainException("UNSUPPORTED_MIMETYPE", "Mídia VIDEO exige tipo MIME de vídeo.", 415);
        }
    }

    private void validateFileSize(long fileSizeBytes, AcervoMediaType mediaType) {
        long maxSize = getMaxSizeForType(mediaType);
        if (fileSizeBytes > maxSize) {
            throw new DomainException("FILE_TOO_LARGE",
                "Arquivo excede o tamanho máximo permitido de " + (maxSize / 1024 / 1024) + "MB.", 413);
        }
    }

    private List<String> getAllowedMimesForType(AcervoMediaType mediaType) {
        if (mediaType == null) return new ArrayList<>();
        return switch (mediaType) {
            case IMAGE -> uploadProperties.getAllowedImageMimes();
            case VIDEO -> uploadProperties.getAllowedVideoMimes();
        };
    }

    private long getMaxSizeForType(AcervoMediaType mediaType) {
        if (mediaType == null) return uploadProperties.getMaxDocumentSizeBytes();
        return switch (mediaType) {
            case IMAGE -> uploadProperties.getMaxImageSizeBytes();
            case VIDEO -> uploadProperties.getMaxVideoSizeBytes();
        };
    }

    private DecisionResponse decision(String code, String message, String resourcePublicKey, String status) {
        return new DecisionResponse(code, message, resourcePublicKey, status, UUID.randomUUID().toString(), OffsetDateTime.now());
    }

    private MediaItemResponse toMediaResponse(MediaEntity entity) {
        List<MediaFileVariantEntity> variants = mediaFileVariantRepository.findByMediaIdOrderByCreatedAtAsc(entity.getId());
        List<AcervoFileResponse> files = variants.stream().map(v -> {
            AcervoFileResponse file = new AcervoFileResponse(
                AcervoFileResponse.VariantEnum.fromValue(v.getVariant()),
                AcervoFileResponse.BucketTypeEnum.fromValue(v.getBucketType()),
                v.getObjectPath(),
                v.getMimeType(),
                v.getSizeBytes()
            );
            file.setWidth(v.getWidth());
            file.setHeight(v.getHeight());
            file.setDurationSeconds(v.getDurationSeconds());
            file.setChecksum(v.getChecksum());
            if (entity.getCdnUrl() != null && AcervoStatus.PUBLICADO.getValue().equals(entity.getStatus())) {
                file.setCdnUrl(URI.create(cdnBaseUrl + "/" + v.getObjectPath()));
            }
            return file;
        }).toList();

        if (files.isEmpty()) {
            AcervoFileResponse fallback = new AcervoFileResponse(
                AcervoFileResponse.VariantEnum.ORIGINAL,
                AcervoStatus.PUBLICADO.getValue().equals(entity.getStatus())
                    ? AcervoFileResponse.BucketTypeEnum.PUBLIC_MEDIA
                    : AcervoFileResponse.BucketTypeEnum.QUARANTINE,
                entity.getObjectPath(),
                entity.getMimeType(),
                entity.getSizeBytes()
            );
            files = List.of(fallback);
        }

        RightsDeclarationResponse rights = new RightsDeclarationResponse()
            .sourceType(MediaSourceType.fromValue(defaultValue(entity.getSourceType(), MediaSourceType.TERCEIRO.getValue())))
            .responsibleDeclaration(entity.getResponsibleDeclaration())
            .hasImageUseAuthorization(entity.getHasImageUseAuthorization())
            .allowsCommercialUse(entity.getAllowsCommercialUse())
            .allowsMarketingUse(entity.getAllowsMarketingUse())
            .allowsSocialMediaUse(entity.getAllowsSocialMediaUse());

        return new MediaItemResponse(
            entity.getMediaPublicKey(),
            EntityType.fromValue(entity.getEntityType()),
            entity.getEntityId(),
            AcervoMediaType.fromValue(entity.getMediaType()),
            AcervoPurpose.fromValue(entity.getPurpose()),
            AcervoVisibility.fromValue(entity.getVisibility()),
            AcervoStatus.fromValue(entity.getStatus()),
            files,
            entity.getCreatedAt(),
            entity.getUpdatedAt())
            .ownerPublicKey(entity.getOwnerPublicKey())
            .boatPublicKey(entity.getBoatPublicKey())
            .accommodationCode(entity.getAccommodationCode())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .main(entity.getMain())
            .displayOrder(entity.getDisplayOrder())
            .rejectionReasonCode(entity.getRejectionReasonCode())
            .rejectionReasonDescription(entity.getRejectionReasonDescription())
            .rights(rights)
            .approvedAt(entity.getApprovedAt())
            .publishedAt(entity.getPublishedAt());
    }

    private PrivateDocumentResponse toDocumentResponse(PrivateDocumentEntity entity) {
        AcervoFileResponse file = new AcervoFileResponse(
            AcervoFileResponse.VariantEnum.DOCUMENT_ORIGINAL,
            AcervoFileResponse.BucketTypeEnum.PRIVATE_DOCUMENTS,
            entity.getObjectPath(),
            entity.getMimeType(),
            entity.getSizeBytes());

        boolean expired = entity.getExpiresAt() != null && entity.getExpiresAt().isBefore(LocalDate.now());
        return new PrivateDocumentResponse(
            entity.getDocumentPublicKey(),
            EntityType.fromValue(entity.getEntityType()),
            entity.getEntityId(),
            entity.getDocumentTypeCode(),
            DocumentStatus.fromValue(entity.getStatus()),
            AcervoVisibility.fromValue(entity.getVisibility()),
            entity.getCreatedAt(),
            entity.getUpdatedAt())
            .documentTypeName(entity.getDocumentTypeName())
            .documentNumber(entity.getDocumentNumber())
            .issuer(entity.getIssuer())
            .issuedAt(entity.getIssuedAt())
            .expiresAt(entity.getExpiresAt())
            .expired(expired)
            .approvedAt(entity.getApprovedAt())
            .file(file);
    }

    private void recordHistory(String itemPublicKey, String itemType, String eventCode, String statusBefore, String statusAfter,
                               String reasonCode, String observation, ActorReference actor, String correlationId) {
        AcervoHistoryEntity history = new AcervoHistoryEntity();
        history.setId(UUID.randomUUID());
        history.setItemPublicKey(itemPublicKey);
        history.setItemType(itemType);
        history.setEventCode(eventCode);
        history.setStatusBefore(statusBefore);
        history.setStatusAfter(statusAfter);
        history.setReasonCode(reasonCode);
        history.setReasonDescription(observation);
        history.setActorType(actor == null || actor.getActorType() == null ? "SERVICE" : actor.getActorType().getValue());
        history.setActorId(actor == null ? "system" : actor.getActorId());
        history.setCorrelationId(correlationId);
        history.setCreatedAt(OffsetDateTime.now());
        historyRepository.save(history);
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveBucketNameByType(String bucketType) {
        if (bucketType == null || bucketType.isBlank()) {
            return acervoProperties.storage().quarantineBucket();
        }

        return switch (bucketType.toUpperCase()) {
            case "PUBLIC_MEDIA" -> acervoProperties.storage().publicMediaBucket();
            case "PRIVATE_DOCUMENTS" -> acervoProperties.storage().privateDocumentsBucket();
            case "QUARANTINE" -> acervoProperties.storage().quarantineBucket();
            default -> acervoProperties.storage().quarantineBucket();
        };
    }

    private MediaFileVariantEntity pickMediaVariant(List<MediaFileVariantEntity> variants, String requestedVariant) {
        if (requestedVariant != null && !requestedVariant.isBlank()) {
            for (MediaFileVariantEntity variant : variants) {
                if (requestedVariant.equalsIgnoreCase(variant.getVariant())) {
                    return variant;
                }
            }
        }

        String[] fallbackOrder = new String[] {"THUMB", "CARD", "GALLERY", "BANNER", "SLIDE", "ORIGINAL"};
        for (String candidate : fallbackOrder) {
            for (MediaFileVariantEntity variant : variants) {
                if (candidate.equalsIgnoreCase(variant.getVariant())) {
                    return variant;
                }
            }
        }

        return variants.get(0);
    }

    private String extractFilename(String objectPath) {
        if (objectPath == null || objectPath.isBlank()) {
            return "arquivo";
        }

        int index = objectPath.lastIndexOf('/');
        if (index < 0 || index + 1 >= objectPath.length()) {
            return objectPath;
        }

        return objectPath.substring(index + 1);
    }

    private void validateFileContent(MultipartFile file, AcervoMediaType mediaType, String declaredMimeType) {
        byte[] bytes = toBytes(file);
        String detectedMimeType;
        try {
            detectedMimeType = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
        } catch (IOException ex) {
            throw new DomainException("INVALID_FILE_CONTENT", "Não foi possível validar o conteúdo do arquivo enviado.", 422);
        }

        if (mediaType == AcervoMediaType.IMAGE) {
            try {
                if (ImageIO.read(new ByteArrayInputStream(bytes)) == null) {
                    throw new DomainException("INVALID_IMAGE_CONTENT", "Conteúdo do arquivo não corresponde a uma imagem válida.", 422);
                }
            } catch (IOException ex) {
                throw new DomainException("INVALID_IMAGE_CONTENT", "Conteúdo do arquivo não corresponde a uma imagem válida.", 422);
            }
        }

        if (detectedMimeType != null && !mimeTypesEquivalent(detectedMimeType, declaredMimeType)) {
            throw new DomainException("MIMETYPE_MISMATCH", "Tipo MIME declarado não corresponde ao conteúdo real do arquivo.", 422);
        }
    }

    private boolean mimeTypesEquivalent(String detected, String declared) {
        if (declared == null || declared.isBlank()) {
            return false;
        }
        if (detected.equalsIgnoreCase(declared)) {
            return true;
        }
        return ("image/jpg".equalsIgnoreCase(detected) && "image/jpeg".equalsIgnoreCase(declared))
            || ("image/jpeg".equalsIgnoreCase(detected) && "image/jpg".equalsIgnoreCase(declared));
    }

    private String hmacSha256Base64Url(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception ex) {
            throw new DomainException("SIGNED_URL_ERROR", "Falha ao assinar URL do documento privado.", 500);
        }
    }

    private boolean shouldApplyWatermark(MediaEntity media, MediaFileVariantEntity variant) {
        return watermarkService.isEnabled()
            && AcervoMediaType.IMAGE.getValue().equals(media.getMediaType())
            && variant.getMimeType() != null
            && variant.getMimeType().startsWith("image/");
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return "sha256:" + HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new DomainException("HASH_ERROR", "Falha ao calcular checksum do arquivo.", 500);
        }
    }

    private void persistMediaVariants(MediaEntity media, byte[] originalContent, String extension) {
        List<MediaFileVariantEntity> variants = new ArrayList<>();

        if (AcervoMediaType.IMAGE.getValue().equals(media.getMediaType())) {
            List<ImageDerivative> derivatives = imageDerivativeService.createDerivatives(originalContent, media.getMimeType());
            for (ImageDerivative derivative : derivatives) {
                String objectPath = mediaObjectPath(media, derivative.variant(), extension);
                objectStoragePort.putObject(acervoProperties.storage().quarantineBucket(), objectPath, derivative.content(), derivative.mimeType());

                MediaFileVariantEntity variant = new MediaFileVariantEntity();
                variant.setId(UUID.randomUUID());
                variant.setMediaId(media.getId());
                variant.setVariant(derivative.variant().getValue());
                variant.setBucketType(AcervoFileResponse.BucketTypeEnum.QUARANTINE.getValue());
                variant.setObjectPath(objectPath);
                variant.setMimeType(derivative.mimeType());
                variant.setSizeBytes(derivative.sizeBytes());
                variant.setWidth(derivative.width());
                variant.setHeight(derivative.height());
                variant.setDurationSeconds(null);
                variant.setChecksum(derivative.checksum());
                variant.setCreatedAt(OffsetDateTime.now());
                variant.setUpdatedAt(OffsetDateTime.now());
                variants.add(variant);
            }

            media.setObjectPath(mediaObjectPath(media, AcervoFileResponse.VariantEnum.ORIGINAL, extension));
            ImageDerivative original = derivatives.stream()
                .filter(d -> d.variant() == AcervoFileResponse.VariantEnum.ORIGINAL)
                .findFirst()
                .orElseThrow(() -> new DomainException("MEDIA_VARIANT_ERROR", "Variante original de imagem não foi gerada.", 500));
            media.setSizeBytes(original.sizeBytes());
            media.setMimeType(original.mimeType());
            mediaRepository.save(media);
        } else {
            String objectPath = mediaObjectPath(media, AcervoFileResponse.VariantEnum.ORIGINAL, extension);
            objectStoragePort.putObject(acervoProperties.storage().quarantineBucket(), objectPath, originalContent, media.getMimeType());

            MediaFileVariantEntity variant = new MediaFileVariantEntity();
            variant.setId(UUID.randomUUID());
            variant.setMediaId(media.getId());
            variant.setVariant(AcervoFileResponse.VariantEnum.ORIGINAL.getValue());
            variant.setBucketType(AcervoFileResponse.BucketTypeEnum.QUARANTINE.getValue());
            variant.setObjectPath(objectPath);
            variant.setMimeType(media.getMimeType());
            variant.setSizeBytes(media.getSizeBytes());
            variant.setWidth(null);
            variant.setHeight(null);
            variant.setDurationSeconds(null);
            variant.setChecksum("sha256:" + Integer.toHexString(new String(originalContent, StandardCharsets.ISO_8859_1).hashCode()));
            variant.setCreatedAt(OffsetDateTime.now());
            variant.setUpdatedAt(OffsetDateTime.now());
            variants.add(variant);
        }

        mediaFileVariantRepository.deleteByMediaId(media.getId());
        mediaFileVariantRepository.saveAll(variants);
    }

    private String mediaObjectPath(MediaEntity media, AcervoFileResponse.VariantEnum variant, String extension) {
        String normalizedExt = extension == null || extension.isBlank() ? "bin" : extension;
        return "quarantine/" + media.getMediaPublicKey() + "/" + variant.getValue().toLowerCase() + "/" + media.getMediaPublicKey() + "." + normalizedExt;
    }

    private String extensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "bin";
        }
        return switch (mimeType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            case "image/tiff" -> "tiff";
            case "video/mp4" -> "mp4";
            case "video/quicktime" -> "mov";
            case "video/x-msvideo" -> "avi";
            case "video/x-matroska" -> "mkv";
            case "application/pdf" -> "pdf";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            default -> "bin";
        };
    }

    private byte[] toBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new DomainException("FILE_READ_ERROR", "Falha ao ler o arquivo enviado.", 500);
        }
    }

    public static final class BinaryContent {
        private final byte[] content;
        private final String mimeType;
        private final String filename;

        public BinaryContent(byte[] content, String mimeType, String filename) {
            this.content = content;
            this.mimeType = mimeType;
            this.filename = filename;
        }

        public byte[] getContent() {
            return content;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getFilename() {
            return filename;
        }
    }

}
