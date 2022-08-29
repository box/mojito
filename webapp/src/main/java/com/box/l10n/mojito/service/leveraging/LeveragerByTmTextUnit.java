package com.box.l10n.mojito.service.leveraging;

import com.box.l10n.mojito.entity.TMTextUnit;
import com.box.l10n.mojito.service.tm.search.StatusFilter;
import com.box.l10n.mojito.service.tm.search.TextUnitDTO;
import com.box.l10n.mojito.service.tm.search.TextUnitSearcherParameters;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Configurable;

/**
 * Performs leveraging based using the provided TmTextUnit id
 *
 * @author jaurambault
 */
@Configurable
public class LeveragerByTmTextUnit extends AbstractLeverager {

  /** logger */
  static Logger logger = LoggerFactory.getLogger(LeveragerByTmTextUnit.class);

  Long tmTextUnitId;

  public LeveragerByTmTextUnit(Long tmTextUnitId) {
    this.tmTextUnitId = tmTextUnitId;
  }

  @Override
  public List<TextUnitDTO> getLeveragingMatches(
      TMTextUnit tmTextUnit, Long sourceTmId, Long sourceAssetId) {
    logger.debug("Get TextUnitDTOs for leveraging with TmTextUnit");

    TextUnitSearcherParameters textUnitSearcherParameters = new TextUnitSearcherParameters();
    textUnitSearcherParameters.setTmTextUnitIds(tmTextUnitId);
    textUnitSearcherParameters.setAssetId(sourceAssetId);
    textUnitSearcherParameters.setStatusFilter(StatusFilter.TRANSLATED);

    return textUnitSearcher.search(textUnitSearcherParameters);
  }

  @Override
  public boolean isTranslationNeededIfUniqueMatch() {
    return true;
  }

  @Override
  public String getType() {
    return "Leverage with TmTextUnit";
  }
}
