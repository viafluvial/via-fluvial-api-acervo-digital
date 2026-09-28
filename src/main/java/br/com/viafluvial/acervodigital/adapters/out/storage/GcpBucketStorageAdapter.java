package br.com.viafluvial.acervodigital.adapters.out.storage;

import br.com.viafluvial.acervodigital.application.ports.ObjectStoragePort;
import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.CopyRequest;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "acervo.storage.provider", havingValue = "gcp")
public class GcpBucketStorageAdapter implements ObjectStoragePort {

    private final Storage storage;

    public GcpBucketStorageAdapter(
        @Value("${acervo.storage.gcp-project-id:}") String gcpProjectId,
        @Value("${acervo.storage.gcp-credentials-file:}") String gcpCredentialsFile
    ) {
        try {
            StorageOptions.Builder builder = StorageOptions.newBuilder();

            if (gcpProjectId != null && !gcpProjectId.isBlank()) {
                builder.setProjectId(gcpProjectId.trim());
            }

            if (gcpCredentialsFile != null && !gcpCredentialsFile.isBlank()) {
                Path credentialsPath = Path.of(gcpCredentialsFile.trim()).toAbsolutePath().normalize();
                try (var in = Files.newInputStream(credentialsPath)) {
                    builder.setCredentials(GoogleCredentials.fromStream(in));
                }
            }

            this.storage = builder.build().getService();
        } catch (IOException ex) {
            throw new DomainException("STORAGE_GCP_CONFIG_ERROR", "Falha ao carregar credenciais do GCP para storage.", 500);
        } catch (Exception ex) {
            throw new DomainException("STORAGE_GCP_INIT_ERROR", "Falha ao inicializar cliente de storage do GCP.", 500);
        }
    }

    @Override
    public void putObject(String bucketName, String objectPath, byte[] content, String mimeType) {
        validateInputs(bucketName, objectPath);
        try {
            BlobId blobId = BlobId.of(bucketName, objectPath);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType((mimeType == null || mimeType.isBlank()) ? "application/octet-stream" : mimeType)
                .build();
            storage.create(blobInfo, content == null ? new byte[0] : content);
        } catch (StorageException ex) {
            throw new DomainException("STORAGE_WRITE_ERROR", "Falha ao gravar arquivo no bucket GCP configurado.", 500);
        }
    }

    @Override
    public byte[] getObject(String bucketName, String objectPath) {
        validateInputs(bucketName, objectPath);
        try {
            Blob blob = storage.get(BlobId.of(bucketName, objectPath));
            if (blob == null || !blob.exists()) {
                throw new DomainException("STORAGE_OBJECT_NOT_FOUND", "Arquivo nao encontrado no bucket GCP configurado.", 404);
            }
            return blob.getContent();
        } catch (StorageException ex) {
            if (ex.getCode() == 404) {
                throw new DomainException("STORAGE_OBJECT_NOT_FOUND", "Arquivo nao encontrado no bucket GCP configurado.", 404);
            }
            throw new DomainException("STORAGE_READ_ERROR", "Falha ao ler arquivo do bucket GCP configurado.", 500);
        }
    }

    @Override
    public void copyObject(String sourceBucketName, String sourceObjectPath, String targetBucketName, String targetObjectPath) {
        validateInputs(sourceBucketName, sourceObjectPath);
        validateInputs(targetBucketName, targetObjectPath);
        try {
            CopyRequest request = CopyRequest.newBuilder()
                .setSource(BlobId.of(sourceBucketName, sourceObjectPath))
                .setTarget(BlobId.of(targetBucketName, targetObjectPath))
                .build();
            storage.copy(request);
        } catch (StorageException ex) {
            throw new DomainException("STORAGE_COPY_ERROR", "Falha ao copiar arquivo entre buckets no GCP.", 500);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectPath) {
        validateInputs(bucketName, objectPath);
        try {
            storage.delete(BlobId.of(bucketName, objectPath));
        } catch (StorageException ex) {
            if (ex.getCode() == 404) {
                return;
            }
            throw new DomainException("STORAGE_DELETE_ERROR", "Falha ao remover arquivo do bucket GCP configurado.", 500);
        }
    }

    private void validateInputs(String bucketName, String objectPath) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new DomainException("STORAGE_BUCKET_INVALID", "Bucket inválido para operação de storage.", 500);
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new DomainException("STORAGE_PATH_INVALID", "Caminho de objeto inválido para operação de storage.", 500);
        }
    }
}
