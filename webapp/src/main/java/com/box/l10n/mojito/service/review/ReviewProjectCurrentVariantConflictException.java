package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.rest.review.ReviewProjectWS.ReviewProjectTextUnitResponse;

public class ReviewProjectCurrentVariantConflictException extends RuntimeException {

  private final Long expectedVariantId;
  private final Long currentVariantId;
  private final ReviewProjectTextUnitResponse currentTextUnit;

  public ReviewProjectCurrentVariantConflictException(
      Long expectedVariantId,
      Long currentVariantId,
      ReviewProjectTextUnitResponse currentTextUnit) {
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

  public ReviewProjectTextUnitResponse getCurrentTextUnit() {
    return currentTextUnit;
  }
}
