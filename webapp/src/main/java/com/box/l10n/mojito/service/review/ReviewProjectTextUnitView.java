package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectStatus;
import com.box.l10n.mojito.entity.review.ReviewProjectType;
import java.time.ZonedDateTime;
import java.util.List;

public record ReviewProjectTextUnitView(
    Long id,
    ReviewProjectType type,
    ReviewProjectStatus status,
    ZonedDateTime createdDate,
    ZonedDateTime dueDate,
    String closeReason,
    Integer textUnitCount,
    Integer wordCount,
    ReviewProjectRequest reviewProjectRequest,
    Locale locale,
    List<ReviewProjectTextUnit> reviewProjectTextUnits) {

  public record ReviewProjectRequest(Long id, String name, List<String> screenshotImageIds) {}

  public record Locale(Long id, String bcp47Tag) {}

  public record ReviewProjectTextUnit(
      Long id, TmTextUnit tmTextUnit, TmTextUnitVariant tmTextUnitVariant) {}

  public record TmTextUnit(
      Long id, String name, String content, String comment, Asset asset, Long wordCount) {}

  public record Asset(String assetPath, Repository repository) {
    public record Repository(Long id, String name) {}
  }

  public record TmTextUnitVariant(
      Long id, String content, String status, boolean includedInLocalizedFile, String comment) {}
}
