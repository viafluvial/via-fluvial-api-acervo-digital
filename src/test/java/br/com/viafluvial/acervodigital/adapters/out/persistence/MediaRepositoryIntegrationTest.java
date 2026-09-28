package br.com.viafluvial.acervodigital.adapters.out.persistence;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaJpaRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("Media Repository Integration Tests")
class MediaRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("acervo_digital_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("db/migration/V001__create_schema.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MediaJpaRepository mediaRepository;

    private MediaEntity testEntity;

    @BeforeEach
    void setUp() {
        testEntity = new MediaEntity();
        testEntity.setId(UUID.randomUUID());
        testEntity.setMediaPublicKey("med_test_" + System.nanoTime());
        testEntity.setEntityType("EMBARCACAO");
        testEntity.setEntityId(UUID.randomUUID());
        testEntity.setMediaType("IMAGEM");
        testEntity.setPurpose("GALERIA");
        testEntity.setVisibility("PUBLICA");
        testEntity.setStatus("AGUARDANDO_APROVACAO");
        testEntity.setMain(false);
        testEntity.setObjectPath("acervo/media/test.jpg");
        testEntity.setMimeType("image/jpeg");
        testEntity.setSizeBytes(1024L);
        testEntity.setPlatformShowcaseEnabled(false);
        testEntity.setCreatedAt(OffsetDateTime.now());
        testEntity.setUpdatedAt(OffsetDateTime.now());
    }

    @Test
    @DisplayName("Should save and retrieve media entity")
    void testSaveAndRetrieveMedia() {
        MediaEntity saved = mediaRepository.save(testEntity);
        assertNotNull(saved.getId());

        Optional<MediaEntity> retrieved = mediaRepository.findByMediaPublicKey(testEntity.getMediaPublicKey());
        assertTrue(retrieved.isPresent());
        assertEquals(testEntity.getMediaPublicKey(), retrieved.get().getMediaPublicKey());
    }

    @Test
    @DisplayName("Should update media entity")
    void testUpdateMedia() {
        MediaEntity saved = mediaRepository.save(testEntity);
        saved.setStatus("APROVADO");
        saved.setUpdatedAt(OffsetDateTime.now());

        MediaEntity updated = mediaRepository.save(saved);
        assertEquals("APROVADO", updated.getStatus());
    }

    @Test
    @DisplayName("Should find media by public key returns empty when not found")
    void testFindByPublicKeyNotFound() {
        Optional<MediaEntity> result = mediaRepository.findByMediaPublicKey("non_existent_key");
        assertTrue(result.isEmpty());
    }
}
