package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

/**
 * Entity that assign an integrity check for a type of asset.
 *
 * @author wyau
 */
@Entity
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@Table(
    name = "asset_integrity_checker",
    indexes = {
      @Index(
          name = "I__ASSET_INTEGRITY_CHECKER__REPOSITORY_ID__ASSET_EXTENSION",
          columnList = "repository_id, asset_extension",
          unique = false)
    })
public class AssetIntegrityChecker extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference
  @Schema(hidden = true)
  @JoinColumn(
      name = "repository_id",
      foreignKey = @ForeignKey(name = "FK__ASSET_INTEGRITY_CHECKER__REPOSITORY__ID"),
      nullable = false)
  private Repository repository;

  @Basic(optional = false)
  @Column(name = "asset_extension")
  @JsonView(View.Repository.class)
  private String assetExtension;

  @Basic(optional = false)
  @Column(name = "integrity_checker_type")
  @Enumerated(EnumType.STRING)
  @JsonView(View.Repository.class)
  private IntegrityCheckerType integrityCheckerType;

  public Repository getRepository() {
    return repository;
  }

  public void setRepository(Repository repository) {
    this.repository = repository;
  }

  public String getAssetExtension() {
    return assetExtension;
  }

  public void setAssetExtension(String assetExtension) {
    this.assetExtension = assetExtension;
  }

  public IntegrityCheckerType getIntegrityCheckerType() {
    return integrityCheckerType;
  }

  public void setIntegrityCheckerType(IntegrityCheckerType integrityCheckerType) {
    this.integrityCheckerType = integrityCheckerType;
  }
}
