package com.box.l10n.mojito.rest.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

/**
 * REST client DTO for one integrity checker on a {@link RepoType}.
 *
 * <p>JSON is only {@code assetExtension} and {@code integrityCheckerType}. Parent and row ids are
 * server housekeeping and are not part of this payload. Uses the same {@link IntegrityCheckerType}
 * enum names as repository checkers so type-level and repo-level configs can be unioned by that
 * pair.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RepoTypeIntegrityChecker {

  private String assetExtension;
  private IntegrityCheckerType integrityCheckerType;

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
