package br.com.viafluvial.acervodigital.adapters.out.persistence.repository;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaJpaRepository extends JpaRepository<MediaEntity, UUID> {
    Optional<MediaEntity> findByMediaPublicKey(String mediaPublicKey);
}
