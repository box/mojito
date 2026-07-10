package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared utilities for leveraging operations. Used by all leverager implementations instead of
 * inheriting from a common base class.
 */
@Component
public class LeveragingUtils {

  @Autowired private TextUnitSearcher textUnitSearcher;

  @Autowired private TMService tmService;

  @Autowired private TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  public TextUnitSearcher getTextUnitSearcher() {
    return textUnitSearcher;
  }

  /**
   * Arbitrarily takes the first TMTextUnit ID in the list and filters to only translations from
   * that TU, for consistency.
   */
  public void filterTextUnitDTOWithSameTMTextUnitId(List<TextUnitDTO> textUnitDTOs) {
    if (textUnitDTOs.size() <= 1) {
      return;
    }
    Long tmTextUnitIdForLeveraging = textUnitDTOs.get(0).getTmTextUnitId();
    textUnitDTOs.removeIf(dto -> !dto.getTmTextUnitId().equals(tmTextUnitIdForLeveraging));
  }

  /**
   * Writes leveraged translations into the target TMTextUnit. Always overwrites (no status checks)
   * — appropriate for source leveraging and other cases where the target TU is newly created or
   * explicitly targeted.
   */
  @Transactional
  public void addLeveragedTranslations(
      TMTextUnit tmTextUnit,
      List<TextUnitDTO> translations,
      boolean translationNeeded,
      boolean uniqueMatch,
      String type) {

    for (TextUnitDTO translation : translations) {
      AddTMTextUnitCurrentVariantResult result =
          tmService.addTMTextUnitCurrentVariantWithResult(
              tmTextUnit.getId(),
              translation.getLocaleId(),
              translation.getTarget(),
              translation.getTargetComment(),
              translationNeeded
                  ? TMTextUnitVariant.Status.TRANSLATION_NEEDED
                  : translation.getStatus(),
              translation.isIncludedInLocalizedFile(),
              null);

      TMTextUnitCurrentVariant currentVariant = result.getTmTextUnitCurrentVariant();

      if (result.isTmTextUnitCurrentVariantUpdated()) {
        tmTextUnitVariantCommentService.copyComments(
            translation.getTmTextUnitVariantId(), currentVariant.getTmTextUnitVariant().getId());

        tmTextUnitVariantCommentService.addComment(
            currentVariant.getTmTextUnitVariant(),
            TMTextUnitVariantComment.Type.LEVERAGING,
            TMTextUnitVariantComment.Severity.INFO,
            type
                + " - leveraging from tmTextUnitId: "
                + translation.getTmTextUnitId()
                + ", tmTextUnitVariantId: "
                + translation.getTmTextUnitVariantId()
                + ", unique match: "
                + uniqueMatch);
      }
    }
  }
}
