package br.com.viafluvial.acervodigital.application.bootstrap;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.PrivateDocumentEntity;
import br.com.viafluvial.acervodigital.adapters.out.persistence.repository.PrivateDocumentJpaRepository;
import br.com.viafluvial.acervodigital.application.ports.ObjectStoragePort;
import br.com.viafluvial.acervodigital.config.AcervoProperties;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dsv")
public class DsvSeedPrivateDocumentBackfill {

    private static final Logger LOGGER = LoggerFactory.getLogger(DsvSeedPrivateDocumentBackfill.class);
    private static final String SEEDED_DOCUMENT_KEY = "doc_seed_0001";

    private final PrivateDocumentJpaRepository privateDocumentRepository;
    private final ObjectStoragePort objectStoragePort;
    private final AcervoProperties acervoProperties;

    public DsvSeedPrivateDocumentBackfill(
        PrivateDocumentJpaRepository privateDocumentRepository,
        ObjectStoragePort objectStoragePort,
        AcervoProperties acervoProperties
    ) {
        this.privateDocumentRepository = privateDocumentRepository;
        this.objectStoragePort = objectStoragePort;
        this.acervoProperties = acervoProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureSeededDocumentBinaryExists() {
        Optional<PrivateDocumentEntity> maybeDocument = privateDocumentRepository.findByDocumentPublicKey(SEEDED_DOCUMENT_KEY);
        if (maybeDocument.isEmpty()) {
            return;
        }

        PrivateDocumentEntity document = maybeDocument.get();
        String bucketName = acervoProperties.storage().privateDocumentsBucket();

        try {
            byte[] existing = objectStoragePort.getObject(bucketName, document.getObjectPath());
            if (existing != null && existing.length > 0) {
                return;
            }
        } catch (Exception ignored) {
            // Seed row exists but file is missing in local storage; recreate below.
        }

        byte[] pdfBytes = seededPdf();
        objectStoragePort.putObject(bucketName, document.getObjectPath(), pdfBytes, "application/pdf");
        LOGGER.info("Backfilled seed document binary for {} at {}", SEEDED_DOCUMENT_KEY, document.getObjectPath());
    }

    private byte[] seededPdf() {
        String content = "%PDF-1.4\n"
            + "1 0 obj<< /Type /Catalog /Pages 2 0 R >>endobj\n"
            + "2 0 obj<< /Type /Pages /Kids [3 0 R] /Count 1 >>endobj\n"
            + "3 0 obj<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources<<>> >>endobj\n"
            + "4 0 obj<< /Length 78 >>stream\n"
            + "BT /F1 12 Tf 72 760 Td (Documento seed de licenca de operacao - Via Fluvial DSV) Tj ET\n"
            + "endstream endobj\n"
            + "xref\n0 5\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000223 00000 n \n"
            + "trailer<< /Size 5 /Root 1 0 R >>\nstartxref\n356\n%%EOF\n";
        return content.getBytes(StandardCharsets.US_ASCII);
    }
}
