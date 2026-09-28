package br.com.viafluvial.acervodigital.adapters.in.web.controller;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.api.CatalogsApi;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.api.DocumentApprovalApi;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.api.DocumentsApi;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.api.MediaApi;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.api.MediaApprovalApi;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.api.QueriesApi;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.*;
import br.com.viafluvial.acervodigital.application.usecase.AcervoApplicationService;
import java.time.LocalDate;
import java.util.Objects;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AcervoController implements
    MediaApi,
    MediaApprovalApi,
    DocumentsApi,
    DocumentApprovalApi,
    CatalogsApi,
    QueriesApi {

    private final AcervoApplicationService service;

    public record PlatformShowcaseSelectionRequest(Boolean enabled, ActorReference changedBy) {
    }

    public record PlatformShowcaseSelectionResponse(String mediaPublicKey, Boolean enabled) {
    }

    public AcervoController(AcervoApplicationService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<MediaItemResponse> _uploadMedia(MultipartFile file, EntityType entityType, UUID entityId, AcervoMediaType mediaType,
                                                         AcervoPurpose purpose, AcervoVisibility visibility, ActorReference uploadedBy,
                                                         String xCorrelationId, String ownerPublicKey, String boatPublicKey,
                                                         String accommodationCode, String title, String description,
                                                         RightsDeclarationRequest rightsDeclaration) {
        MediaItemResponse response = service.uploadMedia(file, entityType, entityId, mediaType, purpose, visibility, uploadedBy,
            ownerPublicKey, boatPublicKey, accommodationCode, title, description, rightsDeclaration);
        return ResponseEntity.status(201)
            .header("Location", "/api/v1/acervo/media/" + response.getMediaPublicKey())
            .body(response);
    }

    @Override
    public ResponseEntity<PagedMediaResponse> _searchMedia(String xCorrelationId, EntityType entityType, UUID entityId, AcervoMediaType mediaType,
                                                          AcervoPurpose purpose, AcervoVisibility visibility, AcervoStatus status,
                                                          Integer page, Integer size, String sort) {
        return ResponseEntity.ok(service.searchMedia(entityType, entityId, mediaType, purpose, visibility, status, page, size));
    }

    @Override
    public ResponseEntity<MediaItemResponse> _getMediaByPublicKey(String mediaPublicKey, String xCorrelationId) {
        return ResponseEntity.ok(service.getMedia(mediaPublicKey));
    }

    @Override
    public ResponseEntity<PagedMediaResponse> _listMediaByEntity(EntityType entityType, UUID entityId, String xCorrelationId,
                                                                Boolean onlyPublished, Integer page, Integer size) {
        return ResponseEntity.ok(service.listMediaByEntity(entityType, entityId, onlyPublished, page, size));
    }

    @GetMapping("/acervo/media/{mediaPublicKey}/showcase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformShowcaseSelectionResponse> getMediaShowcaseSelection(
        @PathVariable String mediaPublicKey
    ) {
        boolean enabled = service.isMediaShowcaseEnabled(mediaPublicKey);
        return ResponseEntity.ok(new PlatformShowcaseSelectionResponse(mediaPublicKey, enabled));
    }

    @PatchMapping("/acervo/media/{mediaPublicKey}/showcase")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PlatformShowcaseSelectionResponse> updateMediaShowcaseSelection(
        @PathVariable String mediaPublicKey,
        @RequestBody(required = false) PlatformShowcaseSelectionRequest request
    ) {
        if (request == null || request.enabled() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campo 'enabled' é obrigatório.");
        }

        boolean enabled = service.updateMediaShowcaseEnabled(mediaPublicKey, request.enabled(), request.changedBy());
        return ResponseEntity.ok(new PlatformShowcaseSelectionResponse(mediaPublicKey, enabled));
    }

    @GetMapping("/acervo/public/hero-media")
    public ResponseEntity<List<MediaItemResponse>> listPublicHeroMedia(
        @RequestParam(name = "limit", required = false) Integer limit
    ) {
        return ResponseEntity.ok(service.listPublicHeroMedia(limit));
    }

    @Override
    public ResponseEntity<DecisionResponse> _approveMedia(String mediaPublicKey, ApprovalDecisionRequest approvalDecisionRequest,
                                                         String xCorrelationId) {
        return ResponseEntity.ok(service.approveMedia(mediaPublicKey, approvalDecisionRequest));
    }

    @Override
    public ResponseEntity<DecisionResponse> _rejectMedia(String mediaPublicKey, RejectionDecisionRequest rejectionDecisionRequest,
                                                        String xCorrelationId) {
        return ResponseEntity.ok(service.rejectMedia(mediaPublicKey, rejectionDecisionRequest));
    }

    @Override
    public ResponseEntity<MediaItemResponse> _publishMedia(String mediaPublicKey, String xCorrelationId, PublishMediaRequest publishMediaRequest) {
        return ResponseEntity.ok(service.publishMedia(mediaPublicKey, publishMediaRequest));
    }

    @Override
    public ResponseEntity<DecisionResponse> _blockMedia(String mediaPublicKey, BlockMediaRequest blockMediaRequest, String xCorrelationId) {
        return ResponseEntity.ok(service.blockMedia(mediaPublicKey, blockMediaRequest));
    }

    @Override
    public ResponseEntity<DecisionResponse> _archiveMedia(String mediaPublicKey, String xCorrelationId, ArchiveRequest archiveRequest) {
        return ResponseEntity.ok(service.archiveMedia(mediaPublicKey, archiveRequest));
    }

    @Override
    public ResponseEntity<MediaItemResponse> _setMainMedia(String mediaPublicKey, SetMainMediaRequest setMainMediaRequest, String xCorrelationId) {
        return ResponseEntity.ok(service.setMainMedia(mediaPublicKey, setMainMediaRequest));
    }

    @Override
    public ResponseEntity<MediaItemResponse> _updateMediaOrder(String mediaPublicKey, UpdateOrderRequest updateOrderRequest, String xCorrelationId) {
        return ResponseEntity.ok(service.updateMediaOrder(mediaPublicKey, updateOrderRequest));
    }

    @Override
    public ResponseEntity<PrivateDocumentResponse> _uploadPrivateDocument(MultipartFile file, EntityType entityType, UUID entityId,
                                                                         String documentTypeCode, ActorReference uploadedBy,
                                                                         String xCorrelationId, String documentNumber,
                                                                         String issuer, LocalDate issuedAt, LocalDate expiresAt) {
        PrivateDocumentResponse response = service.uploadPrivateDocument(file, entityType, entityId, documentTypeCode, uploadedBy,
            documentNumber, issuer, issuedAt, expiresAt);
        return ResponseEntity.status(201)
            .header("Location", "/api/v1/acervo/private-documents/" + response.getDocumentPublicKey())
            .body(response);
    }

    @Override
    public ResponseEntity<PagedPrivateDocumentResponse> _searchPrivateDocuments(String xCorrelationId, EntityType entityType,
                                                                               UUID entityId, String documentTypeCode,
                                                                               DocumentStatus status, LocalDate expiresUntil,
                                                                               Integer page, Integer size) {
        return ResponseEntity.ok(service.searchPrivateDocuments(entityType, entityId, documentTypeCode, status, expiresUntil, page, size));
    }

    @Override
    public ResponseEntity<PrivateDocumentResponse> _getPrivateDocumentByPublicKey(String documentPublicKey, String xCorrelationId) {
        return ResponseEntity.ok(service.getPrivateDocument(documentPublicKey));
    }

    @Override
    public ResponseEntity<DecisionResponse> _removePrivateDocument(String documentPublicKey, String xCorrelationId) {
        return ResponseEntity.ok(service.removePrivateDocument(documentPublicKey));
    }

    @Override
    public ResponseEntity<SignedUrlResponse> _createPrivateDocumentSignedUrl(String documentPublicKey, String xCorrelationId,
                                                                            SignedUrlRequest signedUrlRequest) {
        return ResponseEntity.ok(service.createSignedUrl(documentPublicKey, signedUrlRequest, xCorrelationId));
    }

    @Override
    public ResponseEntity<DecisionResponse> _approvePrivateDocument(String documentPublicKey,
                                                                   ApprovalDecisionRequest approvalDecisionRequest,
                                                                   String xCorrelationId) {
        return ResponseEntity.ok(service.approvePrivateDocument(documentPublicKey, approvalDecisionRequest));
    }

    @Override
    public ResponseEntity<DecisionResponse> _rejectPrivateDocument(String documentPublicKey,
                                                                  RejectionDecisionRequest rejectionDecisionRequest,
                                                                  String xCorrelationId) {
        return ResponseEntity.ok(service.rejectPrivateDocument(documentPublicKey, rejectionDecisionRequest));
    }

    @Override
    public ResponseEntity<List<DocumentTypeResponse>> _listDocumentTypes(String xCorrelationId, DocumentScope scope, Boolean active) {
        return ResponseEntity.ok(service.listDocumentTypes(scope, active));
    }

    @Override
    public ResponseEntity<List<RejectionReasonResponse>> _listRejectionReasons(String xCorrelationId, String targetType) {
        return ResponseEntity.ok(service.listRejectionReasons(targetType));
    }

    @Override
    public ResponseEntity<PagedApprovalQueueResponse> _getApprovalQueue(String xCorrelationId, String itemType,
                                                                       EntityType entityType, UUID entityId, Integer page,
                                                                       Integer size) {
        return ResponseEntity.ok(service.approvalQueue(itemType, entityType, entityId, page, size));
    }

    @Override
    public ResponseEntity<List<AcervoHistoryEntry>> _getAcervoItemHistory(String itemPublicKey, String xCorrelationId) {
        return ResponseEntity.ok(service.itemHistory(itemPublicKey));
    }

    @Override
    public ResponseEntity<PendingItemsReportResponse> _getPendingItemsReport(String xCorrelationId,
                                                                             String ownerPublicKey,
                                                                             String boatPublicKey) {
        return ResponseEntity.ok(service.pendingItemsReport(ownerPublicKey, boatPublicKey));
    }

    @GetMapping("/acervo/media/{mediaPublicKey}/content")
    public ResponseEntity<byte[]> getMediaContent(
        @PathVariable String mediaPublicKey,
        @RequestParam(name = "variant", required = false) String variant
    ) {
        AcervoApplicationService.BinaryContent binary = service.getMediaContent(mediaPublicKey, variant);
        MediaType mediaType = resolveMediaType(binary.getMimeType());

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(mediaType))
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + binary.getFilename() + "\"")
            .body(binary.getContent());
    }

    @GetMapping("/acervo/private-documents/{documentPublicKey}/content")
    public ResponseEntity<byte[]> getPrivateDocumentContent(@PathVariable String documentPublicKey) {
        AcervoApplicationService.BinaryContent binary = service.getPrivateDocumentContent(documentPublicKey);
        MediaType mediaType = resolveMediaType(binary.getMimeType());

        return ResponseEntity.ok()
            .contentType(Objects.requireNonNull(mediaType))
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + binary.getFilename() + "\"")
            .body(binary.getContent());
    }

    private MediaType resolveMediaType(String value) {
        try {
            String safeValue = (value == null || value.isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : value;
            return MediaType.parseMediaType(safeValue);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
