package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonView;
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

/**
 * Persistence row for an integrity checker owned by a {@link RepoType}.
 *
 * <p>This is a separate JPA entity from {@link AssetIntegrityChecker} only because the parent FK
 * differs ({@code repo_type_id} vs {@code repository_id}). At runtime, both expose the same logical
 * identity — {@code (assetExtension, integrityCheckerType)} using the shared {@link
 * IntegrityCheckerType} enum — so a push/import can take the <em>superset</em> of type-level and
 * repo-level checkers by that pair.
 *
 * <p>Multiple checkers may share the same {@code assetExtension} as long as their {@link
 * IntegrityCheckerType} values differ. There is no unique constraint on extension alone.
 */
@Entity
@Table(
    name = "repo_type_integrity_checker",
    indexes = {
      @Index(
          name = "I__REPO_TYPE_INTEGRITY_CHECKER__REPO_TYPE_ID__ASSET_EXTENSION",
          columnList = "repo_type_id, asset_extension",
          unique = false)
    })
public class RepoTypeIntegrityChecker extends BaseEntity {

  /** Owning repo type. Required; omitted from JSON via {@link JsonBackReference}. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JsonBackReference("integrityCheckers")
  @JoinColumn(
      name = "repo_type_id",
      foreignKey = @ForeignKey(name = "FK__REPO_TYPE_INTEGRITY_CHECKER__REPO_TYPE__ID"),
      nullable = false)
  private RepoType repoType;

  /**
   * Asset file extension this checker applies to (e.g. {@code properties}, {@code resw}), without a
   * leading dot. Required.
   */
  @Basic(optional = false)
  @Column(name = "asset_extension")
  @JsonView(View.IdAndName.class)
  private String assetExtension;

  /**
   * Which integrity checker implementation to run. Same enum values as repository checkers.
   * Required.
   */
  @Basic(optional = false)
  @Column(name = "integrity_checker_type")
  @Enumerated(EnumType.STRING)
  @JsonView(View.IdAndName.class)
  private IntegrityCheckerType integrityCheckerType;

  /**
   * @return owning {@link RepoType}
   */
  public RepoType getRepoType() {
    return repoType;
  }

  /**
   * @param repoType owning type; must be set before save
   */
  public void setRepoType(RepoType repoType) {
    this.repoType = repoType;
  }

  /**
   * @return asset extension this checker applies to
   */
  public String getAssetExtension() {
    return assetExtension;
  }

  /**
   * @param assetExtension extension without leading dot; must be non-null when persisted
   */
  public void setAssetExtension(String assetExtension) {
    this.assetExtension = assetExtension;
  }

  /**
   * @return checker type enum value
   */
  public IntegrityCheckerType getIntegrityCheckerType() {
    return integrityCheckerType;
  }

  /**
   * @param integrityCheckerType shared enum used by repo-level checkers as well
   */
  public void setIntegrityCheckerType(IntegrityCheckerType integrityCheckerType) {
    this.integrityCheckerType = integrityCheckerType;
  }
}
