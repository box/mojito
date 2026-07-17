package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.Iterator;
import java.util.List;

/**
 * Leverages translations from a specific TMTextUnit ID into target text units. Used by
 * AssetExtractionService and VirtualTextUnitBatchUpdaterService when a name-based match has been
 * identified through pre-computed in-memory lookups.
 *
 * @author jaurambault
 */
public class LeveragerByTmTextUnit {

  private final Long tmTextUnitId;
  private final LeveragingUtils leveragingUtils;

  public LeveragerByTmTextUnit(Long tmTextUnitId, LeveragingUtils leveragingUtils) {
    this.tmTextUnitId = tmTextUnitId;
    this.leveragingUtils = leveragingUtils;
  }

  public void performLeveragingFor(List<TMTextUnit> tmTextUnits) {
    for (Iterator<TMTextUnit> it = tmTextUnits.iterator(); it.hasNext(); ) {
      TMTextUnit tmTextUnit = it.next();

      TextUnitSearcherParameters params = new TextUnitSearcherParameters();
      params.setTmTextUnitIds(tmTextUnitId);
      params.setStatusFilter(StatusFilter.TRANSLATED);
      List<TextUnitDTO> candidates = leveragingUtils.getTextUnitSearcher().search(params);

      if (candidates.isEmpty()) {
        continue;
      }

      it.remove();

      int sizeBeforeFilter = candidates.size();
      leveragingUtils.filterTextUnitDTOWithSameTMTextUnitId(candidates);
      boolean uniqueMatch = sizeBeforeFilter == candidates.size();
      boolean translationNeeded = true;

      leveragingUtils.addLeveragedTranslations(
          tmTextUnit, candidates, translationNeeded, uniqueMatch, "Leverage with TmTextUnit");
    }
  }
}
