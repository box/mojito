package com.box.l10n.mojito.service.tm;

import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;

/**
 * Result of {@link TMService#addTMTextUnitCurrentVariantWithResult(java.lang.Long, java.lang.Long,
 * java.lang.String, java.lang.String, com.box.l10n.mojito.entity.TMTextUnitVariant.Status, boolean,
 * java.time.ZonedDateTime) }
 *
 * @author jeanaurambault
 */
public class AddTMTextUnitCurrentVariantResult {

  /** Indicates if {@link TMTextUnitCurrentVariant} has been updated/created or not */
  boolean tmTextUnitCurrentVariantUpdated;

  TMTextUnitCurrentVariant tmTextUnitCurrentVariant;

  public AddTMTextUnitCurrentVariantResult(
      boolean tmTextUnitCurrentVariantUpdated, TMTextUnitCurrentVariant tmTextUnitCurrentVariant) {
    this.tmTextUnitCurrentVariantUpdated = tmTextUnitCurrentVariantUpdated;
    this.tmTextUnitCurrentVariant = tmTextUnitCurrentVariant;
  }

  public boolean isTmTextUnitCurrentVariantUpdated() {
    return tmTextUnitCurrentVariantUpdated;
  }

  public void setTmTextUnitCurrentVariantUpdated(boolean tmTextUnitCurrentVariantUpdated) {
    this.tmTextUnitCurrentVariantUpdated = tmTextUnitCurrentVariantUpdated;
  }

  public TMTextUnitCurrentVariant getTmTextUnitCurrentVariant() {
    return tmTextUnitCurrentVariant;
  }

  public void setTmTextUnitCurrentVariant(TMTextUnitCurrentVariant tmTextUnitCurrentVariant) {
    this.tmTextUnitCurrentVariant = tmTextUnitCurrentVariant;
  }
}
