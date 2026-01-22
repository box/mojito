package com.box.l10n.mojito.service.review;

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
    Long tmTextUnitVariantId,
    String tmTextUnitVariantContent,
    TMTextUnitVariant.Status tmTextUnitVariantStatus,
    Boolean tmTextUnitVariantIncludedInLocalizedFile,
    String tmTextUnitVariantComment,
    Long decisionTmTextUnitVariantId,
    String decisionNotes) {}
