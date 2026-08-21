package com.box.l10n.mojito.entity;

import com.box.l10n.mojito.rest.View;
import com.box.l10n.mojito.service.assetintegritychecker.integritychecker.IntegrityCheckerType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonView;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.util.Objects;

/**
 * One integrity checker on a {@link RepoType}: an {@code (assetExtension, integrityCheckerType)}
 * pair.
 *
 * <p>This is not a JPA entity. Parent FK and any table row id are owned by {@link RepoType}'s
 * element collection; they are not part of this type and are not exposed in JSON. Clients get and
 * send de-duplicated sets of this pair only.
 *
 * <p>Logical identity matches repository checkers so a push/import can later take the superset of
 * type-level and repo-level checkers by that pair. Multiple checkers may share the same {@code
 * assetExtension} as long as their {@link IntegrityCheckerType} values differ.
 */
@Embeddable
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepoTypeIntegrityChecker {

  /**
   * Asset file extension this checker applies to (e.g. {@code properties}, {@code resw}), without a
   * leading dot. Required.
   */
  @Column(name = "asset_extension", nullable = false)
  @JsonView(View.RepoType.class)
  private String assetExtension;

  /**
   * Which integrity checker implementation to run. Same enum values as repository checkers.
   * Required.
   */
  @Column(name = "integrity_checker_type", nullable = false)
  @Enumerated(EnumType.STRING)
  @JsonView(View.RepoType.class)
  private IntegrityCheckerType integrityCheckerType;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RepoTypeIntegrityChecker that)) {
      return false;
    }
    return Objects.equals(assetExtension, that.assetExtension)
        && integrityCheckerType == that.integrityCheckerType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(assetExtension, integrityCheckerType);
  }
}
