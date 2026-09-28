package br.com.viafluvial.acervodigital.adapters.out.persistence.repository;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.PrivateDocumentEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivateDocumentJpaRepository extends JpaRepository<PrivateDocumentEntity, UUID> {
    Optional<PrivateDocumentEntity> findByDocumentPublicKey(String documentPublicKey);
}
