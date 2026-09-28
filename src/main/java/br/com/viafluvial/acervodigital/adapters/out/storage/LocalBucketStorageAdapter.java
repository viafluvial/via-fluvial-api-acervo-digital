package br.com.viafluvial.acervodigital.adapters.out.storage;

import br.com.viafluvial.acervodigital.application.ports.ObjectStoragePort;
import br.com.viafluvial.acervodigital.domain.exception.DomainException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "acervo.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalBucketStorageAdapter implements ObjectStoragePort {

    private final Path basePath;

    public LocalBucketStorageAdapter(@Value("${acervo.storage.local-base-path:}") String localBasePath) {
        if (localBasePath == null || localBasePath.isBlank()) {
            throw new DomainException("STORAGE_CONFIG_MISSING", "Configuração acervo.storage.local-base-path é obrigatória para upload de arquivos.", 500);
        }
        this.basePath = Path.of(localBasePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(basePath);
        } catch (IOException ex) {
            throw new DomainException("STORAGE_INIT_ERROR", "Não foi possível inicializar o storage local configurado.", 500);
        }
    }

    @Override
    public void putObject(String bucketName, String objectPath, byte[] content, String mimeType) {
        Path target = resolvePath(bucketName, objectPath);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException ex) {
            throw new DomainException("STORAGE_WRITE_ERROR", "Falha ao gravar arquivo no storage configurado.", 500);
        }
    }

    @Override
    public byte[] getObject(String bucketName, String objectPath) {
        Path source = resolvePath(bucketName, objectPath);
        try {
            return Files.readAllBytes(source);
        } catch (IOException ex) {
            throw new DomainException("STORAGE_OBJECT_NOT_FOUND", "Arquivo nao encontrado no storage configurado.", 404);
        }
    }

    @Override
    public void copyObject(String sourceBucketName, String sourceObjectPath, String targetBucketName, String targetObjectPath) {
        Path source = resolvePath(sourceBucketName, sourceObjectPath);
        Path target = resolvePath(targetBucketName, targetObjectPath);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new DomainException("STORAGE_COPY_ERROR", "Falha ao copiar arquivo entre buckets no storage configurado.", 500);
        }
    }

    @Override
    public void deleteObject(String bucketName, String objectPath) {
        Path target = resolvePath(bucketName, objectPath);
        try {
            Files.delete(target);
        } catch (NoSuchFileException ex) {
            // Remoção idempotente quando o objeto já não existe no storage.
        } catch (IOException ex) {
            throw new DomainException("STORAGE_DELETE_ERROR", "Falha ao remover arquivo do storage configurado.", 500);
        }
    }

    private Path resolvePath(String bucketName, String objectPath) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new DomainException("STORAGE_BUCKET_INVALID", "Bucket inválido para operação de storage.", 500);
        }
        if (objectPath == null || objectPath.isBlank()) {
            throw new DomainException("STORAGE_PATH_INVALID", "Caminho de objeto inválido para operação de storage.", 500);
        }

        Path resolved = basePath.resolve(bucketName).resolve(objectPath).normalize();
        if (!resolved.startsWith(basePath)) {
            throw new DomainException("STORAGE_PATH_INVALID", "Path traversal detectado no object path do storage.", 400);
        }
        return resolved;
    }
}
