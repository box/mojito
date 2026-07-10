package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import com.box.l10n.mojito.service.tm.search.UsedFilter;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Handles source leveraging — copying translations from existing text units in the same asset when
 * new text units are created during a push. This happens automatically after asset extraction.
 *
 * <p>Tries match levels in descending precision order within the same asset:
 *
 * <ol>
 *   <li>name+content from USED TUs (comment-only change, no re-translation needed)
 *   <li>name-only from USED TUs (source text changed, re-translation needed)
 *   <li>content-only from USED TUs (key renamed, no re-translation needed)
 *   <li>name+content from UNUSED TUs (comment-only change on a previously removed TU)
 * </ol>
 *
 * <p>Stops at the first level that yields results. Always overwrites (no status checks) since these
 * are newly created text units with no existing translations.
 */
@Component
public class SourceLeverager {

  static Logger logger = LoggerFactory.getLogger(SourceLeverager.class);

  @Autowired private LeveragingUtils leveragingUtils;

  /**
   * Performs source leveraging for newly created text units. Text units that get leveraged are
   * removed from the list to prevent further processing.
   */
  public void performLeveragingFor(List<TMTextUnit> tmTextUnits) {
    logger.debug("Perform source leveraging for {} text units", tmTextUnits.size());

    for (Iterator<TMTextUnit> it = tmTextUnits.iterator(); it.hasNext(); ) {
      TMTextUnit tmTextUnit = it.next();
      Long assetId = tmTextUnit.getAsset().getId();

      MatchResult match = findBestMatch(tmTextUnit, assetId);
      if (match == null) {
        continue;
      }

      it.remove();

      boolean translationNeeded = match.translationNeeded || !match.uniqueMatch;

      leveragingUtils.addLeveragedTranslations(
          tmTextUnit, match.translations, translationNeeded, match.uniqueMatch, match.type);
    }
  }

  /**
   * Tries each source leveraging strategy in precision order, returning the first that yields
   * results. Searches are scoped to the same asset.
   */
  private MatchResult findBestMatch(TMTextUnit tmTextUnit, Long assetId) {
    List<TextUnitDTO> candidates;

    // 1. Name+content match from USED TUs (comment-only change)
    candidates = searchByNameAndContent(tmTextUnit, assetId, UsedFilter.USED);
    if (!candidates.isEmpty()) {
      return toMatchResult(candidates, false, "by name and content for source leveraging");
    }

    // 2. Name-only match from USED TUs (source text changed)
    candidates = searchByName(tmTextUnit, assetId);
    if (!candidates.isEmpty()) {
      return toMatchResult(candidates, true, "by name for source leveraging");
    }

    // 3. Content-only match from USED TUs (key renamed)
    candidates = searchByContent(tmTextUnit, assetId);
    if (!candidates.isEmpty()) {
      return toMatchResult(candidates, false, "by content for source leveraging");
    }

    // 4. Name+content match from UNUSED TUs (comment-only change on removed TU)
    candidates = searchByNameAndContent(tmTextUnit, assetId, UsedFilter.UNUSED);
    if (!candidates.isEmpty()) {
      return toMatchResult(
          candidates, false, "by name and content from unused for source leveraging");
    }

    return null;
  }

  private MatchResult toMatchResult(
      List<TextUnitDTO> candidates, boolean translationNeeded, String type) {
    int sizeBeforeFilter = candidates.size();
    leveragingUtils.filterTextUnitDTOWithSameTMTextUnitId(candidates);
    boolean uniqueMatch = sizeBeforeFilter == candidates.size();
    return new MatchResult(candidates, translationNeeded, uniqueMatch, type);
  }

  private List<TextUnitDTO> searchByNameAndContent(
      TMTextUnit tmTextUnit, Long assetId, UsedFilter usedFilter) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setName(tmTextUnit.getName());
    params.setSource(tmTextUnit.getContent());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    params.setUsedFilter(usedFilter);
    params.setAssetId(assetId);
    return leveragingUtils.getTextUnitSearcher().search(params);
  }

  private List<TextUnitDTO> searchByName(TMTextUnit tmTextUnit, Long assetId) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setName(tmTextUnit.getName());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    params.setUsedFilter(UsedFilter.USED);
    params.setAssetId(assetId);
    return leveragingUtils.getTextUnitSearcher().search(params);
  }

  private List<TextUnitDTO> searchByContent(TMTextUnit tmTextUnit, Long assetId) {
    TextUnitSearcherParameters params = new TextUnitSearcherParameters();
    params.setSource(tmTextUnit.getContent());
    params.setStatusFilter(StatusFilter.TRANSLATED);
    params.setUsedFilter(UsedFilter.USED);
    params.setAssetId(assetId);
    if (tmTextUnit.getPluralForm() != null) {
      params.setPluralFormId(tmTextUnit.getPluralForm().getId());
    } else {
      params.setPluralFormsExcluded(true);
    }
    return leveragingUtils.getTextUnitSearcher().search(params);
  }

  private record MatchResult(
      List<TextUnitDTO> translations,
      boolean translationNeeded,
      boolean uniqueMatch,
      String type) {}
}
