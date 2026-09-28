package br.com.viafluvial.acervodigital.adapters.out.persistence.repository;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.MediaFileVariantEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileVariantJpaRepository extends JpaRepository<MediaFileVariantEntity, UUID> {

    List<MediaFileVariantEntity> findByMediaIdOrderByCreatedAtAsc(UUID mediaId);

    void deleteByMediaId(UUID mediaId);
}
