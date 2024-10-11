package com.box.l10n.mojito.service.ai.translation;

/**
 * DTO class that is used as part of the AI Translation flow, specifically for the mapping of locale
 * and variant IDs.
 *
 * <p>It is used when setting the current variants for a given locale and text unit.
 *
 * @author maallen
 */
public class LocaleVariantDTO {

  private Long localeId;
  private Long variantId;

  public LocaleVariantDTO(Long localeId, Long variantId) {
    this.localeId = localeId;
    this.variantId = variantId;
  }

  public Long getLocaleId() {
    return localeId;
  }

  public void setLocaleId(Long localeId) {
    this.localeId = localeId;
  }

  public Long getVariantId() {
    return variantId;
  }

  public void setVariantId(Long variantId) {
    this.variantId = variantId;
  }
}
