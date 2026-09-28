package br.com.viafluvial.acervodigital.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.ActorReference;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.ApprovalDecisionRequest;
import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.DecisionResponse;
import br.com.viafluvial.acervodigital.adapters.out.integration.VesselOwnershipClient;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.AcervoHistoryJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.DocumentTypeCatalogJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaFileVariantJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.PrivateDocumentJpaRepository;
import br.com.viafluvial.acervodigital.application.media.ImageDerivativeService;
import br.com.viafluvial.acervodigital.application.media.WatermarkService;
import br.com.viafluvial.acervodigital.application.ports.ObjectStoragePort;
import br.com.viafluvial.acervodigital.application.usecase.AcervoApplicationService;
import br.com.viafluvial.acervodigital.common.id.CurrentActorProvider;
import br.com.viafluvial.acervodigital.config.AcervoProperties;
import br.com.viafluvial.acervodigital.config.AcervoUploadProperties;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AcervoApplicationServiceTest {

    @Mock
    private MediaJpaRepository mediaRepository;
    @Mock
    private PrivateDocumentJpaRepository documentRepository;
    @Mock
    private MediaFileVariantJpaRepository mediaFileVariantRepository;
    @Mock
    private AcervoHistoryJpaRepository historyRepository;

    @Mock
    private DocumentTypeCatalogJpaRepository documentTypeCatalogRepository;

    @Mock
    private br.com.viafluvial.acervodigital.adapters.out.persistence.repository.RejectionReasonCatalogJpaRepository rejectionReasonCatalogRepository;

    @Mock
    private ObjectStoragePort objectStoragePort;

    @Mock
    private ImageDerivativeService imageDerivativeService;

    @Mock
    private WatermarkService watermarkService;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private VesselOwnershipClient vesselOwnershipClient;

    private AcervoApplicationService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AcervoApplicationService(
            mediaRepository,
            mediaFileVariantRepository,
            documentRepository,
            historyRepository,
            documentTypeCatalogRepository,
            rejectionReasonCatalogRepository,
            new AcervoUploadProperties(),
            new AcervoProperties(
                new AcervoProperties.Api("/api/v1"),
                new AcervoProperties.Storage("local", "q-bucket", "p-bucket", "d-bucket", "https://cdn.example", "./storage", null, null)
            ),
            objectStoragePort,
            imageDerivativeService,
            watermarkService,
            currentActorProvider,
            vesselOwnershipClient
        );
    }

    @Test
    void shouldApproveMedia() {
        MediaEntity media = new MediaEntity();
        media.setId(UUID.randomUUID());
        media.setMediaPublicKey("med_test");
        media.setStatus("AGUARDANDO_APROVACAO");
        media.setEntityType("EMBARCACAO");
        media.setEntityId(UUID.randomUUID());
        media.setMediaType("IMAGE");
        media.setPurpose("GALERIA");
        media.setVisibility("PUBLICA");
        media.setObjectPath("quarantine/med_test/file.jpg");
        media.setMimeType("image/jpeg");
        media.setSizeBytes(1000L);
        media.setCreatedAt(OffsetDateTime.now());
        media.setUpdatedAt(OffsetDateTime.now());

        when(mediaRepository.findByMediaPublicKey("med_test")).thenReturn(Optional.of(media));
        when(mediaRepository.save(any(MediaEntity.class))).thenAnswer(i -> i.getArgument(0));

        ApprovalDecisionRequest request = new ApprovalDecisionRequest(
            new ActorReference(ActorReference.ActorTypeEnum.ADMIN, "admin_1"));

        DecisionResponse response = service.approveMedia("med_test", request);

        assertThat(response.getCode()).isEqualTo("MEDIA_APPROVED");
        assertThat(response.getStatus()).isEqualTo("APROVADO");
    }
}
