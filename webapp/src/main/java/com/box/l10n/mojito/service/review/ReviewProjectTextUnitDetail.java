package com.box.l10n.mojito.service.review;

import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.review.ReviewProjectTextUnitDecision.DecisionState;

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
    Long decisionVariantId,
    String decisionVariantContent,
    TMTextUnitVariant.Status decisionVariantStatus,
    Boolean decisionVariantIncludedInLocalizedFile,
    String decisionVariantComment,
    Long reviewedTmTextUnitVariantId,
    String decisionNotes,
    DecisionState decisionState) {}
