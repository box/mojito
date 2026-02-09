package com.box.l10n.mojito.service.review;

public class ReviewProjectCurrentVariantConflictException extends RuntimeException {

  private final Long expectedVariantId;
  private final Long currentVariantId;
  private final ReviewProjectTextUnitDetail currentTextUnit;

  public ReviewProjectCurrentVariantConflictException(
      Long expectedVariantId, Long currentVariantId, ReviewProjectTextUnitDetail currentTextUnit) {
    super("Current TM text unit variant changed");
    this.expectedVariantId = expectedVariantId;
    this.currentVariantId = currentVariantId;
    this.currentTextUnit = currentTextUnit;
  }

  public Long getExpectedVariantId() {
    return expectedVariantId;
  }

  public Long getCurrentVariantId() {
    return currentVariantId;
  }

  public ReviewProjectTextUnitDetail getCurrentTextUnit() {
    return currentTextUnit;
  }
}
