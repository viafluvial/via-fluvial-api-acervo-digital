package br.com.viafluvial.acervodigital.adapters.out.persistence.repository;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.RejectionReasonCatalogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RejectionReasonCatalogJpaRepository extends JpaRepository<RejectionReasonCatalogEntity, String> {
    List<RejectionReasonCatalogEntity> findByTargetType(String targetType);
    List<RejectionReasonCatalogEntity> findByTargetTypeAndActive(String targetType, Boolean active);
    List<RejectionReasonCatalogEntity> findByActive(Boolean active);
}
