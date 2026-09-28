package br.com.viafluvial.acervodigital.application.usecase;

import br.com.viafluvial.acervodigital.adapters.in.web.generated.model.*;
import br.com.viafluvial.acervodigital.adapters.out.integration.VesselOwnershipClient;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.AcervoHistoryJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.PrivateDocumentJpaRepository;
import br.com.viafluvial.acervodigital.common.id.CurrentActorProvider;
import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Acervo Application Service Domain Tests")
class AcervoApplicationServiceDomainTest {

    @Mock
    private MediaJpaRepository mediaRepository;

    @Mock
    private PrivateDocumentJpaRepository documentRepository;

    @Mock
    private AcervoHistoryJpaRepository historyRepository;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private VesselOwnershipClient vesselOwnershipClient;

    @InjectMocks
    private AcervoApplicationService service;

    private MediaEntity approvedMedia;

    @BeforeEach
    void setUp() {
        approvedMedia = new MediaEntity();
        approvedMedia.setId(UUID.randomUUID());
        approvedMedia.setMediaPublicKey("med_test_123");
        approvedMedia.setStatus("APROVADO");
        approvedMedia.setVisibility("PUBLICA");
        approvedMedia.setCreatedAt(OffsetDateTime.now());
        approvedMedia.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should throw exception when publishing non-approved media")
    void testPublishNonApprovedMediaThrows() {
        approvedMedia.setStatus("AGUARDANDO_APROVACAO");
        when(mediaRepository.findByMediaPublicKey("med_test_123")).thenReturn(Optional.of(approvedMedia));

        DomainException ex = assertThrows(DomainException.class, () ->
            service.publishMedia("med_test_123", new PublishMediaRequest())
        );

        assertEquals("MEDIA_NOT_APPROVED", ex.getCode());
        assertEquals(409, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("Should throw exception when publishing private media")
    void testPublishPrivateMediaThrows() {
        approvedMedia.setVisibility("PRIVADA");
        when(mediaRepository.findByMediaPublicKey("med_test_123")).thenReturn(Optional.of(approvedMedia));

        DomainException ex = assertThrows(DomainException.class, () ->
            service.publishMedia("med_test_123", new PublishMediaRequest())
        );

        assertEquals("MEDIA_NOT_PUBLIC", ex.getCode());
        assertEquals(422, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("Should throw 404 when media not found")
    void testGetNonExistentMediaThrows() {
        when(mediaRepository.findByMediaPublicKey("non_existent")).thenReturn(Optional.empty());

        DomainException ex = assertThrows(DomainException.class, () ->
            service.getMedia("non_existent")
        );

        assertEquals("MEDIA_NOT_FOUND", ex.getCode());
        assertEquals(404, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("Should throw 400 when file is null")
    void testUploadNullFileThrows() {
        DomainException ex = assertThrows(DomainException.class, () ->
            service.uploadMedia(null, EntityType.EMBARCACAO, UUID.randomUUID(), AcervoMediaType.IMAGE,
                AcervoPurpose.GALERIA, AcervoVisibility.PUBLICA, null, null, null, null, null, null, null)
        );

        assertEquals("INVALID_FILE", ex.getCode());
        assertEquals(400, ex.getHttpStatusCode());
    }

    @Test
    @DisplayName("Should deny boatman from accessing another boatman's vessel media")
    void testListMediaByEntityCrossBoatmanForbidden() {
        UUID actorId = UUID.randomUUID();
        UUID vesselId = UUID.randomUUID();

        when(currentActorProvider.isBoatmanScopedActor()).thenReturn(true);
        when(currentActorProvider.currentActorId()).thenReturn(actorId);
        when(vesselOwnershipClient.resolveBoatmanId(vesselId)).thenReturn(UUID.randomUUID());

        DomainException ex = assertThrows(DomainException.class, () ->
            service.listMediaByEntity(EntityType.EMBARCACAO, vesselId, false, 0, 10)
        );

        assertEquals("ACERVO_SCOPE_FORBIDDEN", ex.getCode());
        assertEquals(403, ex.getHttpStatusCode());
    }
}
