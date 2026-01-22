package com.box.l10n.mojito.service.review;

public class ReviewProjectCurrentVariantConflictException extends RuntimeException {

  private final Long expectedVariantId;
  private final Long currentVariantId;
  private final ReviewProjectDetail.ReviewProjectTextUnit currentTextUnit;

  public ReviewProjectCurrentVariantConflictException(
      Long expectedVariantId,
      Long currentVariantId,
      ReviewProjectDetail.ReviewProjectTextUnit currentTextUnit) {
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

  public ReviewProjectDetail.ReviewProjectTextUnit getCurrentTextUnit() {
    return currentTextUnit;
  }
}
