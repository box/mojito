package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision.DecisionState;
import com.box.l10n.mojito.entity.TMTextUnitVariant;

public record ReviewProjectTextUnitDetail(
    Long reviewProjectTextUnitId,
    Long tmTextUnitId,
    String tmTextUnitName,
    String tmTextUnitContent,
    String tmTextUnitComment,
    Integer tmTextUnitWordCount,
    String assetPath,
    Long repositoryId,
    String repositoryName,
    Long baselineTmTextUnitVariantId,
    String baselineTmTextUnitVariantContent,
    TMTextUnitVariant.Status baselineTmTextUnitVariantStatus,
    Boolean baselineTmTextUnitVariantIncludedInLocalizedFile,
    String baselineTmTextUnitVariantComment,
    Long currentTmTextUnitVariantId,
    String currentTmTextUnitVariantContent,
    TMTextUnitVariant.Status currentTmTextUnitVariantStatus,
    Boolean currentTmTextUnitVariantIncludedInLocalizedFile,
    String currentTmTextUnitVariantComment,
    DecisionTmTextUnitVariant decisionTmTextUnitVariant,
    Long reviewedTmTextUnitVariantId,
    String decisionNotes,
    DecisionState decisionState) {

  public record DecisionTmTextUnitVariant(
      Long id,
      String content,
      TMTextUnitVariant.Status status,
      Boolean includedInLocalizedFile,
      String comment) {}
}
