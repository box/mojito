package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;

/**
 * REST client DTO for one integrity checker on a {@link RepoType}.
 *
 * <p>Mirrors the server payload fields ({@code id}, {@code assetExtension}, {@code
 * integrityCheckerType}). Uses the same {@link IntegrityCheckerType} enum names as repository
 * checkers so type-level and repo-level configs can be unioned by {@code (assetExtension,
 * integrityCheckerType)}.
 */
public class RepoTypeIntegrityChecker {

  private Long id;

  /** Parent type; omitted when serializing nested under {@link RepoType}. */
  @JsonBackReference("integrityCheckers")
  private RepoType repoType;

  private String assetExtension;
  private IntegrityCheckerType integrityCheckerType;

  /**
   * @return server-assigned id, or {@code null} for a new checker in a create/update body
   */
  public Long getId() {
    return id;
  }

  /**
   * @param id server-assigned id
   */
  public void setId(Long id) {
    this.id = id;
  }

  /**
   * @return owning {@link RepoType}, if set
   */
  public RepoType getRepoType() {
    return repoType;
  }

  /**
   * @param repoType owning type
   */
  public void setRepoType(RepoType repoType) {
    this.repoType = repoType;
  }

  /**
   * @return asset extension without leading dot
   */
  public String getAssetExtension() {
    return assetExtension;
  }

  /**
   * @param assetExtension e.g. {@code properties}, {@code resw}
   */
  public void setAssetExtension(String assetExtension) {
    this.assetExtension = assetExtension;
  }

  /**
   * @return checker type; same enum as repository checkers
   */
  public IntegrityCheckerType getIntegrityCheckerType() {
    return integrityCheckerType;
  }

  /**
   * @param integrityCheckerType shared enum value
   */
  public void setIntegrityCheckerType(IntegrityCheckerType integrityCheckerType) {
    this.integrityCheckerType = integrityCheckerType;
  }
}
