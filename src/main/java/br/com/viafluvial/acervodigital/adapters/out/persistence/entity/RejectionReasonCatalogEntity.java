package br.com.viafluvial.acervodigital.adapters.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rejection_reason_catalog", schema = "sc-acervo-digital")
public class RejectionReasonCatalogEntity {

    @Id
    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "description", nullable = false, length = 300)
    private String description;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
