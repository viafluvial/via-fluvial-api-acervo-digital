package br.com.viafluvial.acervodigital.adapters.out.persistence.repository;

import br.com.viafluvial.acervodigital.adapters.out.persistence.entity.DocumentTypeCatalogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentTypeCatalogJpaRepository extends JpaRepository<DocumentTypeCatalogEntity, String> {
    List<DocumentTypeCatalogEntity> findByScope(String scope);
    List<DocumentTypeCatalogEntity> findByScopeAndActive(String scope, Boolean active);
    List<DocumentTypeCatalogEntity> findByActive(Boolean active);
}
