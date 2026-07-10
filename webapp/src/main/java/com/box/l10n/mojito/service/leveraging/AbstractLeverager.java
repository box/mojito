package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TM;
import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.entity.TMTextUnitCurrentVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariant;
import com.box.l10n.mojito.entity.TMTextUnitVariantComment;
import com.box.l10n.mojito.service.tm.AddTMTextUnitCurrentVariantResult;
import com.box.l10n.mojito.service.tm.TMService;
import com.box.l10n.mojito.service.tm.TMTextUnitVariantCommentService;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcher;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for simple leveraging strategies that match on a single criterion (e.g. by TmTextUnit
 * ID, or by content+repository). Subclasses implement {@link #getLeveragingMatches} to define the
 * search and {@link #isTranslationNeededIfUniqueMatch} to control status.
 *
 * <p>Used by {@link LeveragerByTmTextUnit} and {@link LeveragerByContentAndRepository}. Source
 * leveraging and copyTm leveraging have their own dedicated classes ({@link SourceLeverager},
 * {@link CopyTmLeverager}).
 *
 * @author jaurambault
 */
public abstract class AbstractLeverager {

  static Logger logger = LoggerFactory.getLogger(AbstractLeverager.class);

  @Autowired protected TextUnitSearcher textUnitSearcher;

  @Autowired TMService tmService;

  @Autowired TMTextUnitVariantCommentService tmTextUnitVariantCommentService;

  /**
   * Gets {@link TextUnitDTO}s that match the {@link TMTextUnit} based on criteria defined by the
   * implementing class.
   *
   * @param tmTextUnit the {@link TMTextUnit}
   * @param sourceTmId the {@link TM#id} of TM to look for matches in (can be null)
   * @param sourceAssetId the Asset ID to look for matches in (can be null)
   * @return a list of {@link TextUnitDTO}s for leveraging
   */
  public abstract List<TextUnitDTO> getLeveragingMatches(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId);

  /**
   * Indicates if the translations must be flagged for re-translation regardless of whether the
   * match is unique.
   */
  public abstract boolean isTranslationNeededIfUniqueMatch();

  public abstract String getType();

  /**
   * Performs leveraging for a list of {@link TMTextUnit}s. For each text unit, searches for
   * matches, filters to a single source TMTextUnit, and copies translations. Text units that get
   * leveraged are removed from the list.
   *
   * <p>Always overwrites existing translations (no status checks) — this is appropriate for the
   * remaining subclasses which leverage into newly created or explicitly targeted text units.
   */
  public void performLeveragingFor(List<TMTextUnit> tmTextUnits, Long sourceTmId, Long assetId) {

    logger.debug("Perform leveraging: {}", getType());

    for (Iterator<TMTextUnit> it = tmTextUnits.iterator(); it.hasNext(); ) {
      TMTextUnit tmTextUnit = it.next();

      List<TextUnitDTO> candidates = getLeveragingMatches(tmTextUnit, sourceTmId, assetId);

      if (candidates.isEmpty()) {
        continue;
      }

      it.remove();

      int sizeBeforeFilter = candidates.size();
      filterTextUnitDTOWithSameTMTextUnitId(candidates);
      boolean uniqueMatch = sizeBeforeFilter == candidates.size();

      boolean translationNeeded = isTranslationNeededIfUniqueMatch() || !uniqueMatch;

      addLeveragedTranslations(tmTextUnit, candidates, translationNeeded, uniqueMatch);
    }
  }

  @Transactional
  private void addLeveragedTranslations(
      TMTextUnit tmTextUnit,
      List<TextUnitDTO> translations,
      boolean translationNeeded,
      boolean uniqueMatch) {

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
            getType()
                + " - leveraging from tmTextUnitId: "
                + translation.getTmTextUnitId()
                + ", tmTextUnitVariantId: "
                + translation.getTmTextUnitVariantId()
                + ", unique match: "
                + uniqueMatch);
      }
    }
  }

  /**
   * Arbitrarily takes the first TMTextUnit ID in the list and filters to only translations from
   * that TU, for consistency.
   */
  protected void filterTextUnitDTOWithSameTMTextUnitId(List<TextUnitDTO> textUnitDTOs) {
    if (textUnitDTOs.size() <= 1) {
      return;
    }
    Long tmTextUnitIdForLeveraging = textUnitDTOs.get(0).getTmTextUnitId();
    textUnitDTOs.removeIf(dto -> !dto.getTmTextUnitId().equals(tmTextUnitIdForLeveraging));
  }
}
