package br.com.viafluvial.acervodigital.application.bootstrap;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.PrivateDocumentEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.MediaJpaRepository;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.PrivateDocumentJpaRepository;
import br.com.viafluvial.acervodigital.application.ports.ObjectStoragePort;
import br.com.viafluvial.acervodigital.common.utils.AssetNamingUtils;
import br.com.viafluvial.acervodigital.config.AcervoProperties;
import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("dsv")
public class DsvAssetNamingBackfill {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsvAssetNamingBackfill.class);

    private final MediaJpaRepository mediaRepository;
    private final PrivateDocumentJpaRepository privateDocumentRepository;
    private final ObjectStoragePort objectStoragePort;
    private final AcervoProperties acervoProperties;

    public DsvAssetNamingBackfill(
        MediaJpaRepository mediaRepository,
        PrivateDocumentJpaRepository privateDocumentRepository,
        ObjectStoragePort objectStoragePort,
        AcervoProperties acervoProperties
    ) {
        this.mediaRepository = mediaRepository;
        this.privateDocumentRepository = privateDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.acervoProperties = acervoProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void backfill() {
        backfillMediaTitles();
        backfillPrivateDocumentPaths();
    }

    private void backfillMediaTitles() {
        List<MediaEntity> all = mediaRepository.findAll();
        int changed = 0;

        for (MediaEntity item : all) {
            String expected = AssetNamingUtils.buildMediaTitle(
                item.getMediaType(),
                item.getEntityType(),
                item.getEntityId(),
                item.getMediaPublicKey(),
                item.getCreatedAt(),
                null
            );

            if (!expected.equals(item.getTitle())) {
                item.setTitle(expected);
                item.setUpdatedAt(OffsetDateTime.now());
                changed++;
            }
        }

        if (changed > 0) {
            mediaRepository.saveAll(all);
            LOGGER.info("Asset naming backfill: {} titulos de midia padronizados.", changed);
        }
    }

    private void backfillPrivateDocumentPaths() {
        List<PrivateDocumentEntity> all = privateDocumentRepository.findAll();
        int changed = 0;

        for (PrivateDocumentEntity item : all) {
            String extension = AssetNamingUtils.extensionFromPath(item.getObjectPath());
            if (extension.isBlank()) {
                extension = extensionFromMimeType(item.getMimeType());
            }

            String expectedFileName = AssetNamingUtils.buildPrivateDocumentFileName(
                item.getEntityType(),
                item.getEntityId(),
                item.getDocumentTypeCode(),
                item.getDocumentPublicKey(),
                item.getCreatedAt(),
                extension
            );
            String expectedPath = AssetNamingUtils.buildPrivateDocumentObjectPath(item.getDocumentPublicKey(), expectedFileName);

            if (expectedPath.equals(item.getObjectPath())) {
                continue;
            }

            if (!copyIfExists(item.getObjectPath(), expectedPath, item.getMimeType())) {
                LOGGER.warn(
                    "Asset naming backfill: nao foi possivel migrar documento {} de {} para {} (arquivo origem ausente).",
                    item.getDocumentPublicKey(),
                    item.getObjectPath(),
                    expectedPath
                );
                continue;
            }

            item.setObjectPath(expectedPath);
            item.setUpdatedAt(OffsetDateTime.now());
            changed++;
        }

        if (changed > 0) {
            privateDocumentRepository.saveAll(all);
            LOGGER.info("Asset naming backfill: {} caminhos de documento privado padronizados.", changed);
        }
    }

    private boolean copyIfExists(String sourcePath, String targetPath, String mimeType) {
        String bucket = acervoProperties.storage().privateDocumentsBucket();
        try {
            byte[] source = objectStoragePort.getObject(bucket, sourcePath);
            String contentType = (mimeType == null || mimeType.isBlank()) ? "application/octet-stream" : mimeType;
            objectStoragePort.putObject(bucket, targetPath, source, contentType);
            return true;
        } catch (DomainException ex) {
            if (ex.getHttpStatusCode() == 404) {
                try {
                    objectStoragePort.getObject(bucket, targetPath);
                    return true;
                } catch (DomainException targetEx) {
                    if (targetEx.getHttpStatusCode() == 404) {
                        return false;
                    }
                    throw targetEx;
                }
            }
            throw ex;
        }
    }

    private String extensionFromMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return "bin";
        }
        return switch (mimeType) {
            case "application/pdf" -> "pdf";
            case "application/msword" -> "doc";
            case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> "docx";
            default -> "bin";
        };
    }
}
