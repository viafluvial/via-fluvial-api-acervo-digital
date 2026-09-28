package br.com.viafluvial.acervodigital.adapters.out.persistence.repository;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.AcervoHistoryEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcervoHistoryJpaRepository extends JpaRepository<AcervoHistoryEntity, UUID> {
    List<AcervoHistoryEntity> findByItemPublicKeyOrderByCreatedAtDesc(String itemPublicKey);
}
