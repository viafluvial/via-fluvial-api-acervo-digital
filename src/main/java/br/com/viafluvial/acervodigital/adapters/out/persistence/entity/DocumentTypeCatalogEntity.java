package br.com.viafluvial.acervodigital.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "document_type_catalog", schema = "sc-acervo-digital")
public class DocumentTypeCatalogEntity {

    @Id
    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "scope", nullable = false, length = 50)
    private String scope;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "requires_expiration", nullable = false)
    private Boolean requiresExpiration;

    @Column(name = "allows_commercial_use", nullable = false)
    private Boolean allowsCommercialUse;

    @Column(name = "allows_social_media_use", nullable = false)
    private Boolean allowsSocialMediaUse;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public Boolean getRequiresExpiration() { return requiresExpiration; }
    public void setRequiresExpiration(Boolean requiresExpiration) { this.requiresExpiration = requiresExpiration; }
    public Boolean getAllowsCommercialUse() { return allowsCommercialUse; }
    public void setAllowsCommercialUse(Boolean allowsCommercialUse) { this.allowsCommercialUse = allowsCommercialUse; }
    public Boolean getAllowsSocialMediaUse() { return allowsSocialMediaUse; }
    public void setAllowsSocialMediaUse(Boolean allowsSocialMediaUse) { this.allowsSocialMediaUse = allowsSocialMediaUse; }
}
